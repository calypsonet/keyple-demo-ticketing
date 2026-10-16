/* ******************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.reload.remote.data.service

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.calypsonet.keyple.demo.reload.remote.data.dto.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.data.dto.RemoteServiceOutputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndAnalyzeContractsInputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndAnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndLoadContractInputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndLoadContractOutputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndPersonalizeCardInputDto
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndPersonalizeCardOutputDto
import org.calypsonet.keyple.demo.reload.remote.data.network.KeypleServerConfig
import org.calypsonet.keyple.demo.reload.remote.data.network.SimpleHttpNetworkClient
import org.calypsonet.keyple.demo.reload.remote.data.network.buildHttpClient
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleResult
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleTerminal
import org.eclipse.keyple.interop.jsonapi.client.api.ServerIOException
import org.eclipse.keyple.interop.jsonapi.client.api.Status
import org.eclipse.keyple.interop.jsonapi.client.spi.LocalReader

private const val SELECT_APP_AND_PERSONALIZE_CARD = "SELECT_APP_AND_PERSONALIZE_CARD"
private const val SELECT_APP_AND_ANALYZE_CONTRACTS = "SELECT_APP_AND_ANALYZE_CONTRACTS"
private const val SELECT_APP_AND_LOAD_CONTRACT = "SELECT_APP_AND_LOAD_CONTRACT"

/**
 * Integration point of the Keyple Interop libraries (JSON API client and local reader): creates the
 * Keyple terminal, provides it with the card selection scenario of the ticketing server, drives the
 * local reader and executes the remote services of the server on the presented card.
 */
class KeypleInteropService(
    private val reader: LocalReader,
    private val clientId: String,
    private val serverConfigRepository: ServerConfigRepository
) {

  private var serverConfig: KeypleServerConfig? = null
  private var keypleTerminal: KeypleTerminal? = null
  private val httpClient = HttpClient {
    install(ContentNegotiation) {
      json(
          Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
          })
    }
    install(HttpTimeout) {
      requestTimeoutMillis = 15000
      socketTimeoutMillis = 16000
    }
    expectSuccess = true
    followRedirects = true
  }
  // Background task of the service (retrieval of the card selection scenario), living as long as
  // the service
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  suspend fun start() {
    serverConfig = serverConfigRepository.load()

    val networkClient = SimpleHttpNetworkClient(serverConfig!!, buildHttpClient(LogLevel.ALL))

    keypleTerminal =
        KeypleTerminal(reader = reader, clientId = clientId, networkClient = networkClient)

    launchCardSelectionScenarioRetrieval()
  }

  private fun launchCardSelectionScenarioRetrieval() {
    keypleTerminal?.let { terminal ->
      scope.launch {
        try {
          val cardSelectionScenario = retrieveCardSelectionScenario()
          Napier.d("Card selection scenario received [scenario=$cardSelectionScenario]")
          terminal.setCardSelectionScenarioJsonString(cardSelectionScenario)
        } catch (e: Exception) {
          Napier.e("Error while getting the card selection scenario", e)
        }
      }
    }
  }

  private suspend fun retrieveCardSelectionScenario(): String {
    return try {
      httpClient
          .get("${serverConfig?.baseUrl()}/card/export-card-selection-scenario") {}
          .body<String>()
    } catch (e: Exception) {
      throw ServerIOException("Communication error [cause=${e.message}]")
    }
  }

  suspend fun waitForCard(): Boolean {
    return withContext(Dispatchers.IO) {
      return@withContext keypleTerminal?.waitForCard() ?: false
    }
  }

  fun waitForCard(onCardConnected: () -> Unit) {
    this.keypleTerminal?.waitForCard(onCardConnected)
  }

  fun setScanMessage(message: String) {
    keypleTerminal?.setScanMessage(message)
  }

  fun releaseReader() {
    keypleTerminal?.releaseReader()
  }

  suspend fun selectAppAndAnalyzeContracts(): KeypleResult<SelectAppAndAnalyzeContractsOutputDto> {
    return withContext(Dispatchers.IO) {
      keypleTerminal?.let { terminal ->
        val result: KeypleResult<SelectAppAndAnalyzeContractsOutputDto> =
            executeRemoteService(
                terminal,
                SELECT_APP_AND_ANALYZE_CONTRACTS,
                SelectAppAndAnalyzeContractsInputDto(),
                SelectAppAndAnalyzeContractsInputDto.serializer(),
                SelectAppAndAnalyzeContractsOutputDto.serializer())

        when (result) {
          is KeypleResult.Failure -> {
            return@withContext result
          }
          is KeypleResult.Success -> {
            if (result.data.statusCode != 0) {
              Napier.w(
                  "Contracts reading failed [statusCode=${result.data.statusCode}, " +
                      "message=${result.data.message}]")
              return@withContext KeypleResult.Failure(
                  status = Status.SERVER_ERROR, message = result.data.message)
            }
            Napier.i(
                "Contracts read [serialNumber=${result.data.applicationSerialNumber}, " +
                    "contracts=${result.data.contracts.size}]")
            return@withContext KeypleResult.Success(result.data)
          }
        }
      } ?: throw IllegalStateException("Remote service not initialized")
    }
  }

  suspend fun selectAppAndPersonalizeCard(): KeypleResult<String> {
    return withContext(Dispatchers.IO) {
      keypleTerminal?.let { terminal ->
        val result =
            executeRemoteServiceWithStatus(
                terminal,
                SELECT_APP_AND_PERSONALIZE_CARD,
                SelectAppAndPersonalizeCardInputDto(),
                SelectAppAndPersonalizeCardInputDto.serializer(),
                SelectAppAndPersonalizeCardOutputDto.serializer(),
                successLog = "Card personalized",
                failureLog = "Personalization failed")
        when (result) {
          is KeypleResult.Failure -> {
            return@withContext result
          }
          is KeypleResult.Success -> {
            return@withContext KeypleResult.Success("Success")
          }
        }
      } ?: throw IllegalStateException("Remote service not initialized")
    }
  }

  /**
   * Loads a contract in the presented card, the server checking that it is the card read before.
   *
   * @param expectedSerialNumber The serial number of the card read before.
   */
  suspend fun selectAppAndLoadContract(
      expectedSerialNumber: String,
      tripsToLoad: Int,
      contractTariff: PriorityCode
  ): KeypleResult<String> {
    return withContext(Dispatchers.IO) {
      keypleTerminal?.let { terminal ->
        val result =
            executeRemoteServiceWithStatus(
                terminal,
                SELECT_APP_AND_LOAD_CONTRACT,
                SelectAppAndLoadContractInputDto(
                    applicationSerialNumber = expectedSerialNumber,
                    contractTariff = contractTariff,
                    tripsToLoad = tripsToLoad),
                SelectAppAndLoadContractInputDto.serializer(),
                SelectAppAndLoadContractOutputDto.serializer(),
                successLog =
                    "Contract loaded [contractTariff=$contractTariff, tripsToLoad=$tripsToLoad]",
                failureLog = "Contract loading failed")
        when (result) {
          is KeypleResult.Failure -> {
            return@withContext result
          }
          is KeypleResult.Success -> {
            return@withContext KeypleResult.Success("Success")
          }
        }
      } ?: throw IllegalStateException("Remote service not initialized")
    }
  }

  /**
   * Executes a remote service whose output only carries a status, and returns its message on
   * success.
   *
   * @param successLog The log message of a success.
   * @param failureLog The log message of a failure, completed by the status code and the message of
   *   the server.
   */
  private suspend fun <T, R : RemoteServiceOutputDto> executeRemoteServiceWithStatus(
      terminal: KeypleTerminal,
      serviceId: String,
      inputData: T? = null,
      inputSerializer: KSerializer<T>,
      outputSerializer: KSerializer<R>,
      successLog: String,
      failureLog: String,
  ): KeypleResult<String> {
    when (val result =
        terminal.executeRemoteService(serviceId, inputData, inputSerializer, outputSerializer)) {
      is KeypleResult.Failure -> {
        Napier.w(
            "Remote service execution failed [service=$serviceId, status=${result.status}, " +
                "message=${result.message}, output=${result.data}]")
        return KeypleResult.Failure(
            status = result.status, message = result.data?.message ?: "Communication error")
      }
      is KeypleResult.Success -> {
        if (result.data != null) {
          if (result.data!!.statusCode != 0) {
            Napier.w(
                "$failureLog [statusCode=${result.data!!.statusCode}, " +
                    "message=${result.data!!.message}]")
            return KeypleResult.Failure(
                status = Status.SERVER_ERROR, message = result.data!!.message)
          } else {
            Napier.i(successLog)
            return KeypleResult.Success(result.data!!.message)
          }
        } else {
          return KeypleResult.Success("")
        }
      }
    }
  }

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
  }

  private suspend fun <T, R> executeRemoteService(
      terminal: KeypleTerminal,
      serviceId: String,
      inputData: T? = null,
      inputSerializer: KSerializer<T>,
      outputSerializer: KSerializer<R>,
  ): KeypleResult<R> {

    var inputDataJson: String? = null
    inputData?.let { inputDataJson = json.encodeToString(inputSerializer, inputData) }

    when (val result = terminal.executeRemoteService(serviceId, inputDataJson)) {
      is KeypleResult.Failure -> {
        Napier.w(
            "Remote service execution failed [service=$serviceId, status=${result.status}, " +
                "message=${result.message}, output=${result.data}]")
        return KeypleResult.Failure(result.status, "Communication error")
      }
      is KeypleResult.Success -> {
        val outputJson: String = result.data!!
        val output = Json.decodeFromString(outputSerializer, outputJson)
        return KeypleResult.Success(output)
      }
    }
  }
}
