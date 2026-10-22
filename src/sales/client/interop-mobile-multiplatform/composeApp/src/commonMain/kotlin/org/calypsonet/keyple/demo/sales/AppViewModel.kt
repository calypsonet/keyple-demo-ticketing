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
package org.calypsonet.keyple.demo.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.sales.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.sales.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.sales.data.service.ServerReadinessChecker

data class AppState(val isServerReady: Boolean = false)

class AppViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val serverReadinessChecker: ServerReadinessChecker,
    private val serverConfigRepository: ServerConfigRepository
) : ViewModel() {

  private var _state = MutableStateFlow(AppState())
  val state = _state.asStateFlow()

  init {
    viewModelScope.launch { keypleInteropService.start() }

    // Periodic check of the server, its configuration being read at each check (address changed in
    // the settings)
    viewModelScope.launch {
      var wasServerReady: Boolean? = null
      while (true) {
        val serverConfig = serverConfigRepository.load()
        var cause = "SAM not available"
        val isServerReady =
            try {
              serverReadinessChecker.isServerReady(serverConfig)
            } catch (e: Exception) {
              // Logged when the readiness of the server changes
              cause = e.message ?: e.toString()
              false
            }
        // Only the changes of the readiness are logged, the server being checked periodically
        if (isServerReady != wasServerReady) {
          if (isServerReady) {
            Napier.i("Server ready [url=${serverConfig.baseUrl()}]")
          } else {
            Napier.w("Server not ready [url=${serverConfig.baseUrl()}, cause=$cause]")
          }
          wasServerReady = isServerReady
        }
        _state.value = AppState(isServerReady = isServerReady)
        delay(SERVER_CHECK_PERIOD_MS)
      }
    }
  }

  private companion object {
    /** Period of the check of the server. */
    const val SERVER_CHECK_PERIOD_MS = 5000L
  }
}
