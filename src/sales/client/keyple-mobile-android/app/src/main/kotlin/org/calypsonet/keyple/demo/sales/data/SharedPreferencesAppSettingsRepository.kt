/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.sales.data

import android.content.SharedPreferences
import org.calypsonet.keyple.demo.sales.domain.model.ServerConfig
import org.calypsonet.keyple.demo.sales.domain.spi.AppSettingsRepository

/** Application settings persisted in the shared preferences. */
class SharedPreferencesAppSettingsRepository(private val preferences: SharedPreferences) :
    AppSettingsRepository {

  override var serverConfig: ServerConfig
    get() =
        ServerConfig(
            // A protocol which is not "http" or "https" (for example, written as "http://" by a
            // previous version) is replaced by the default one
            protocol =
                preferences.getString(SERVER_PROTOCOL_KEY, null)?.takeIf { it in PROTOCOLS }
                    ?: DEFAULT_PROTOCOL,
            host =
                preferences.getString(SERVER_HOST_KEY, DEFAULT_SERVER_HOST) ?: DEFAULT_SERVER_HOST,
            port = preferences.getInt(SERVER_PORT_KEY, DEFAULT_PORT))
    set(value) {
      preferences
          .edit()
          .putString(SERVER_PROTOCOL_KEY, value.protocol)
          .putString(SERVER_HOST_KEY, value.host)
          .putInt(SERVER_PORT_KEY, value.port)
          .apply()
    }

  override var wasServerReady: Boolean
    get() = preferences.getBoolean(WAS_SERVER_READY_KEY, false)
    set(value) {
      preferences.edit().putBoolean(WAS_SERVER_READY_KEY, value).apply()
    }

  companion object {
    private const val SERVER_HOST_KEY = "server_host"
    private const val SERVER_PORT_KEY = "server_port"
    private const val SERVER_PROTOCOL_KEY = "server_protocol"
    private const val DEFAULT_SERVER_HOST = "192.168.0.1"
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_PROTOCOL = "http"
    private val PROTOCOLS = setOf("http", "https")
    private const val WAS_SERVER_READY_KEY = "was_server_ready"
  }
}
