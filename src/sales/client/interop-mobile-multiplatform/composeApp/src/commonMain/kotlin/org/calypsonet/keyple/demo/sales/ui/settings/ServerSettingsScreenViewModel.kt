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
package org.calypsonet.keyple.demo.sales.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.sales.data.network.KeypleServerConfig
import org.calypsonet.keyple.demo.sales.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.sales.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.sales.data.service.ServerReadinessChecker

/** Readiness of the server entered by the user, tested with the "Server state" button. */
enum class ServerReadiness {
  CHECKING,
  READY,
  NOT_READY
}

data class ServerSettingsScreenState(
    val host: String = "",
    val port: String = "",
    val protocol: String = "http",
    val endpoint: String = "",
    val serverReadiness: ServerReadiness? = null,
    val isLoaded: Boolean = false
) {
  val validatedPort: Int?
    get() = port.trim().toIntOrNull()?.takeIf { it in 1..65535 }

  /** True if the host is an IPv4 address or a host name, without protocol nor port. */
  val isHostValid: Boolean
    get() = isIpv4Address(host.trim()) || isHostName(host.trim())

  /** True if the protocol is "http" or "https". */
  val isProtocolValid: Boolean
    get() = protocol.trim() in PROTOCOLS

  /** True if the endpoint is a path of the server, starting with "/". */
  val isEndpointValid: Boolean
    get() = endpoint.trim().let { it.startsWith("/") && it.none { char -> char.isWhitespace() } }

  /** True if all the entries are valid. */
  val isValid: Boolean
    get() = isHostValid && validatedPort != null && isProtocolValid && isEndpointValid

  /** Returns the server configuration entered by the user, or null if an entry is not valid. */
  fun toServerConfig(): KeypleServerConfig? =
      validatedPort
          ?.takeIf { isValid }
          ?.let {
            KeypleServerConfig(
                host = "${protocol.trim()}://${host.trim()}", port = it, endpoint = endpoint.trim())
          }

  private companion object {
    val PROTOCOLS = setOf("http", "https")

    /** Returns true if [host] is an IPv4 address: four numbers from 0 to 255 separated by dots. */
    fun isIpv4Address(host: String): Boolean {
      val parts = host.split('.')
      return parts.size == 4 &&
          parts.all { part ->
            part.length in 1..3 && part.all { it in '0'..'9' } && part.toInt() <= 255
          }
    }

    /** Returns true if [host] is a host name: labels of letters, digits, and hyphens. */
    fun isHostName(host: String): Boolean =
        host.isNotEmpty() &&
            host.any { it.isLetter() } &&
            host.split('.').all { label ->
              label.length in 1..63 &&
                  label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' } &&
                  !label.startsWith("-") &&
                  !label.endsWith("-")
            }
  }
}

class ServerSettingsScreenViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val serverConfigRepository: ServerConfigRepository,
    private val serverReadinessChecker: ServerReadinessChecker
) : ViewModel() {
  private var _state = MutableStateFlow(ServerSettingsScreenState())
  val state = _state.asStateFlow()

  init {
    // The screen displays the saved configuration each time it is opened
    viewModelScope.launch {
      val serverConfig = serverConfigRepository.observe().first()
      val hostUrl = Url(serverConfig.host)
      _state.value =
          ServerSettingsScreenState(
              host = hostUrl.host,
              port = serverConfig.port.toString(),
              endpoint = serverConfig.endpoint,
              protocol = hostUrl.protocol.name,
              isLoaded = true)
    }
  }

  /**
   * Saves the server settings entered by the user, then calls [onSaved]. The new server is used
   * from the next request, the connection of the Keyple Interop libraries being restarted.
   */
  fun saveServerSettings(onSaved: () -> Unit) {
    val settings = state.value
    val port = settings.validatedPort ?: return
    if (!settings.isValid) {
      return
    }
    viewModelScope.launch {
      serverConfigRepository.update(
          host = settings.host.trim(),
          port = port,
          protocol = settings.protocol.trim(),
          endpoint = settings.endpoint.trim(),
      )
      keypleInteropService.start()
      onSaved()
    }
  }

  /** Tests whether the server entered by the user is ready (reachable, with its SAM available). */
  fun testServerReadiness() {
    val serverConfig = state.value.toServerConfig() ?: return
    Napier.i("Checking the server [url=${serverConfig.baseUrl()}]")
    _state.value = state.value.copy(serverReadiness = ServerReadiness.CHECKING)
    viewModelScope.launch {
      val isServerReady =
          try {
            val isServerReady = serverReadinessChecker.isServerReady(serverConfig)
            if (!isServerReady) {
              Napier.w("Server not ready [url=${serverConfig.baseUrl()}, cause=SAM not available]")
            }
            isServerReady
          } catch (e: Exception) {
            Napier.w("Server not ready [url=${serverConfig.baseUrl()}, cause=${e.message}]")
            false
          }
      _state.value =
          state.value.copy(
              serverReadiness =
                  if (isServerReady) ServerReadiness.READY else ServerReadiness.NOT_READY)
    }
  }

  fun onHostChanged(host: String) {
    _state.value = state.value.copy(host = host, serverReadiness = null)
  }

  fun onPortChanged(port: String) {
    _state.value = state.value.copy(port = port, serverReadiness = null)
  }

  fun onProtocolChanged(protocol: String) {
    _state.value = state.value.copy(protocol = protocol, serverReadiness = null)
  }

  fun onEndpointChanged(endpoint: String) {
    _state.value = state.value.copy(endpoint = endpoint, serverReadiness = null)
  }
}
