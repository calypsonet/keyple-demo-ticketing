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
package org.calypsonet.keyple.demo.reload.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerReadinessChecker

data class AppState(val serverOnline: Boolean = false)

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
      while (true) {
        val serverReady =
            try {
              serverReadinessChecker.isServerReady(serverConfigRepository.load())
            } catch (e: Exception) {
              Napier.w("Server not ready [cause=${e.message}]")
              false
            }
        _state.value = AppState(serverOnline = serverReady)
        delay(SERVER_CHECK_PERIOD_MS)
      }
    }
  }

  private companion object {
    /** Period of the check of the server. */
    const val SERVER_CHECK_PERIOD_MS = 5000L
  }
}
