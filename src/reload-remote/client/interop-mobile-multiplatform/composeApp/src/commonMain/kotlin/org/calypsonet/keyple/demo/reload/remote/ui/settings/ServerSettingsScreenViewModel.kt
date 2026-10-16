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
package org.calypsonet.keyple.demo.reload.remote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerConfigRepository

data class ServerSettingsScreenState(
    val host: String = "",
    val port: String = "",
    val protocol: String = "http",
    val endpoint: String = ""
) {
  val validatedPort: Int?
    get() = port.trim().toIntOrNull()?.takeIf { it in 1..65535 }
}

class ServerSettingsScreenViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val serverConfigRepository: ServerConfigRepository
) : ViewModel() {
  private var _state = MutableStateFlow(ServerSettingsScreenState())
  val state = _state.asStateFlow()

  init {
    viewModelScope.launch {
      serverConfigRepository.observe().collect {
        val hostUrl = Url(it.host)
        _state.value =
            ServerSettingsScreenState(
                host = hostUrl.host,
                port = it.port.toString(),
                endpoint = it.endpoint,
                protocol = hostUrl.protocol.name)
      }
    }
  }

  fun applyServerSettings() {
    val settings = state.value
    val port = settings.validatedPort ?: return
    viewModelScope.launch {
      serverConfigRepository.update(
          host = settings.host,
          port = port,
          protocol = settings.protocol,
          endpoint = settings.endpoint,
      )
      keypleInteropService.start()
    }
  }

  fun onHostChanged(host: String) {
    _state.value = state.value.copy(host = host)
  }

  fun onPortChanged(port: String) {
    _state.value = state.value.copy(port = port)
  }

  fun onProtocolChanged(protocol: String) {
    _state.value = state.value.copy(protocol = protocol)
  }

  fun onEndpointChanged(endpoint: String) {
    _state.value = state.value.copy(endpoint = endpoint)
  }
}
