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
    val port: Int = 0,
    val protocol: String = "http",
    val endpoint: String = ""
)

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
                port = it.port,
                endpoint = it.endpoint,
                protocol = hostUrl.protocol.name)
      }
    }
  }

  fun applyServerSettings() {
    viewModelScope.launch {
      serverConfigRepository.update(
          host = state.value.host,
          port = state.value.port,
          protocol = state.value.protocol,
          endpoint = state.value.endpoint,
      )
      keypleInteropService.start()
    }
  }

  fun onHostChanged(host: String) {
    _state.value = state.value.copy(host = host)
  }

  fun onPortChanged(port: Int) {
    _state.value = state.value.copy(port = port)
  }

  fun onProtocolChanged(protocol: String) {
    _state.value = state.value.copy(protocol = protocol)
  }

  fun onEndpointChanged(endpoint: String) {
    _state.value = state.value.copy(endpoint = endpoint)
  }
}
