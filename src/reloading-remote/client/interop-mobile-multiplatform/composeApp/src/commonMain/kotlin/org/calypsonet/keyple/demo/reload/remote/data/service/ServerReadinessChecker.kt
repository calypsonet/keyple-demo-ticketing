/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.calypsonet.keyple.demo.reload.remote.data.network.KeypleServerConfig

/**
 * Checks that the ticketing server is ready to process the cards: reachable, and with its SAM ready
 * (SAM status of the REST API of the server), as the `ServerReadinessChecker` of the Android
 * client.
 */
class ServerReadinessChecker {

  private val httpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) {
      requestTimeoutMillis = 15000
      socketTimeoutMillis = 16000
    }
    expectSuccess = true
  }

  /**
   * Checks the server at the provided address.
   *
   * @param serverConfig The address of the server.
   * @return True if the server and its SAM are ready, false if the SAM is not ready.
   * @throws Exception If the server cannot be reached.
   */
  suspend fun isServerReady(serverConfig: KeypleServerConfig): Boolean =
      httpClient
          .get("${serverConfig.baseUrl()}/card/sam-availability")
          .body<SamAvailability>()
          .isSamAvailable

  /** SAM status returned by the REST API of the server. */
  @Serializable private data class SamAvailability(val isSamAvailable: Boolean)
}
