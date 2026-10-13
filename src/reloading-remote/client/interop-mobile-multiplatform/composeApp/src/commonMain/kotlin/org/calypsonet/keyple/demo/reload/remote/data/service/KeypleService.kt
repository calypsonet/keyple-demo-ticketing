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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.calypsonet.keyple.demo.reload.remote.data.platform.Buzzer
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleResult
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleTerminal
import org.eclipse.keyple.interop.jsonapi.client.api.ServerIOException
import org.eclipse.keyple.interop.jsonapi.client.api.Status
import org.eclipse.keyple.interop.jsonapi.client.spi.LocalReader

private const val SELECT_APP_AND_PERSONALIZE_CARD = "SELECT_APP_AND_PERSONALIZE_CARD"
private const val SELECT_APP_AND_ANALYZE_CONTRACTS = "SELECT_APP_AND_ANALYZE_CONTRACTS"
private const val SELECT_APP_AND_LOAD_CONTRACT = "SELECT_APP_AND_LOAD_CONTRACT"

data class KeypleServiceState(val serverReachable: Boolean = false)

private const val TAG = "KeypleService"

class KeypleService(
    private val reader: LocalReader,
    private val clientId: String,
    private val serverConfigRepository: ServerConfigRepository,
    private val cardRepository: CardRepository,
    private val buzzer: Buzzer
) {

  private var _state: MutableStateFlow<KeypleServiceState> = MutableStateFlow(KeypleServiceState())
  val state = _state.asStateFlow()

  private var serverConfig: KeypleServerConfig? = null
  private var remoteService: KeypleTerminal? = null
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
  // Background tasks of the service (server ping, card selection scenario), living as long as the
  // service: a failing task does not cancel the others
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var pingJob: Job? = null

  suspend fun start() {
    serverConfig = serverConfigRepository.load()

    val server = SimpleHttpNetworkClient(serverConfig!!, buildHttpClient(LogLevel.ALL))

    remoteService = KeypleTerminal(reader = reader, clientId = clientId, networkClient = server)

    launchPingJob()
    launchGetCardSelectionScenarioJob()
  }

  private fun launchPingJob() {
    pingJob?.cancel()
    pingJob =
        remoteService?.let {
          scope.launch {
            while (true) {
              try {
                val response = pingServer()
                _state.value = _state.value.copy(serverReachable = true)
                Napier.d("Ping response: $response")
              } catch (e: Exception) {
                Napier.e("Error pinging server: ${e.message}")
                _state.value = _state.value.copy(serverReachable = false)
              }
              kotlinx.coroutines.delay(5000)
            }
          }
        }
  }

  private fun launchGetCardSelectionScenarioJob() {
    remoteService?.let {
      scope.launch {
        try {
          val scenarioJsonString = retrieveSelectionScenarioJson()
          Napier.d("Card Selection Scenario retrieved: $scenarioJsonString")
          remoteService?.setCardSelectionScenarioJsonString(scenarioJsonString)
        } catch (e: Exception) {
          Napier.e("Error getting card selection scenario: ${e.message}")
        }
      }
    }
  }

  private suspend fun pingServer(): String {
    return try {
      httpClient.get("${serverConfig?.baseUrl()}/card/sam-status") {}.body<String>()
    } catch (e: Exception) {
      throw ServerIOException("Comm error: ${e.message}")
    }
  }

  private suspend fun retrieveSelectionScenarioJson(): String {
    return try {
      httpClient
          .get("${serverConfig?.baseUrl()}/card/export-card-selection-scenario") {}
          .body<String>()
    } catch (e: Exception) {
      throw ServerIOException("Comm error: ${e.message}")
    }
  }

  fun buzzer() {
    buzzer.beepAndVibrate()
  }

  suspend fun waitCard(): Boolean {
    return withContext(Dispatchers.IO) {
      val result = remoteService?.waitForCard() ?: false
      buzzer()
      return@withContext result
    }
  }

  fun waitForCard(cardConnected: () -> Unit) {
    this.remoteService?.waitForCard(cardConnected)
  }

  fun updateReaderMessage(msg: String) {
    remoteService?.setScanMessage(msg)
  }

  fun releaseReader() {
    remoteService?.releaseReader()
  }

  suspend fun selectCardAndAnalyzeContracts(): KeypleResult<SelectAppAndAnalyzeContractsOutputDto> {
    return withContext(Dispatchers.IO) {
      cardRepository.clear()
      remoteService?.let { service ->
        val result: KeypleResult<SelectAppAndAnalyzeContractsOutputDto> =
            executeService(
                service,
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
              Napier.i(tag = TAG, message = "Output = ${result.data.message}")
              return@withContext KeypleResult.Failure(
                  status = Status.SERVER_ERROR,
                  message = "Server side error: ${result.data.statusCode} / ${result.data.message}")
            }
            cardRepository.saveCardSerial(result.data.applicationSerialNumber)
            cardRepository.saveCardContracts(result.data.contracts)
            return@withContext KeypleResult.Success(result.data)
          }
        }
      } ?: throw IllegalStateException("Remote service not initialized")
    }
  }

  suspend fun personalizeCard(): KeypleResult<String> {
    return withContext(Dispatchers.IO) {
      remoteService?.let {
        val result =
            executeServiceWithStatusOutput(
                it,
                SELECT_APP_AND_PERSONALIZE_CARD,
                SelectAppAndPersonalizeCardInputDto(),
                SelectAppAndPersonalizeCardInputDto.serializer(),
                SelectAppAndPersonalizeCardOutputDto.serializer())
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

  suspend fun selectCardAndLoadContract(
      tripsToLoad: Int,
      code: PriorityCode
  ): KeypleResult<String> {
    return withContext(Dispatchers.IO) {
      remoteService?.let {
        val result =
            executeServiceWithStatusOutput(
                it,
                SELECT_APP_AND_LOAD_CONTRACT,
                SelectAppAndLoadContractInputDto(
                    applicationSerialNumber = cardRepository.getCardSerial(),
                    contractTariff = code,
                    tripsToLoad = tripsToLoad),
                SelectAppAndLoadContractInputDto.serializer(),
                SelectAppAndLoadContractOutputDto.serializer())
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
   */
  private suspend fun <T, R : RemoteServiceOutputDto> executeServiceWithStatusOutput(
      remote: KeypleTerminal,
      service: String,
      inputData: T? = null,
      inputSerializer: KSerializer<T>,
      outputSerializer: KSerializer<R>,
  ): KeypleResult<String> {
    when (val result =
        remote.executeRemoteService(service, inputData, inputSerializer, outputSerializer)) {
      is KeypleResult.Failure -> {
        Napier.e(
            tag = TAG,
            message = "Error executing service: ${result.status} ${result.message} ${result.data}")
        return KeypleResult.Failure(
            status = result.status,
            message =
                "Error is ${result.status} - Server side: ${result.data?.statusCode} / ${result.data?.message}")
      }
      is KeypleResult.Success -> {
        if (result.data != null) {
          if (result.data!!.statusCode != 0) {
            Napier.i(tag = TAG, message = "Output = ${result.data!!.message}")
            return KeypleResult.Failure(
                status = Status.SERVER_ERROR,
                message =
                    "Server side error: ${result.data!!.statusCode} / ${result.data!!.message}")
          } else {
            Napier.i(tag = TAG, message = "Output = ${result.data!!.message}")
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

  private suspend fun <T, R> executeService(
      remote: KeypleTerminal,
      service: String,
      inputData: T? = null,
      inputSerializer: KSerializer<T>,
      outputSerializer: KSerializer<R>,
  ): KeypleResult<R> {

    var inputDataStr: String? = null
    inputData?.let { inputDataStr = json.encodeToString(inputSerializer, inputData) }

    when (val result = remote.executeRemoteService(service, inputDataStr)) {
      is KeypleResult.Failure -> {
        Napier.e(
            tag = TAG,
            message = "Error executing service: ${result.status} ${result.message}, ${result.data}")
        return KeypleResult.Failure(result.status, result.message)
      }
      is KeypleResult.Success -> {
        val resStr: String = result.data!!
        val res = Json.decodeFromString(outputSerializer, resStr)
        return KeypleResult.Success(res)
      }
    }
  }
}
