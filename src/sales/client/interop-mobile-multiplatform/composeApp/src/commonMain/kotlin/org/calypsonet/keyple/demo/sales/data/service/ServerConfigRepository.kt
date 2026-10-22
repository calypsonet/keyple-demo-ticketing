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
package org.calypsonet.keyple.demo.sales.data.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.calypsonet.keyple.demo.sales.data.network.KeypleServerConfig

/**
 * Server configuration of the client, persisted in the preferences data store: address of the
 * server (protocol, host, and port), endpoint of the remote plugin and basic authentication.
 */
class ServerConfigRepository(private val dataStore: DataStore<Preferences>) {

  /** Returns the current server configuration, including the basic authentication. */
  suspend fun load(): KeypleServerConfig {
    val preferences = dataStore.data.first()
    return KeypleServerConfig(
        host = hostUrl(preferences),
        port = preferences[SERVER_PORT_KEY] ?: DEFAULT_SERVER_PORT,
        endpoint = preferences[SERVER_ENDPOINT_KEY] ?: DEFAULT_SERVER_ENDPOINT,
        basicAuth = preferences[SERVER_BASIC_AUTH_KEY] ?: "")
  }

  /** Observes the server configuration (without the basic authentication), to edit it. */
  fun observe(): Flow<KeypleServerConfig> =
      dataStore.data.map { preferences ->
        KeypleServerConfig(
            host = hostUrl(preferences),
            port = preferences[SERVER_PORT_KEY] ?: DEFAULT_SERVER_PORT,
            endpoint = preferences[SERVER_ENDPOINT_KEY] ?: DEFAULT_SERVER_ENDPOINT)
      }

  /** Saves the address of the server and the endpoint of the remote plugin. */
  suspend fun update(host: String, port: Int, protocol: String, endpoint: String) {
    dataStore.edit { preferences ->
      preferences[SERVER_HOST_KEY] = host
      preferences[SERVER_PORT_KEY] = port
      preferences[SERVER_PROTOCOL_KEY] = protocol
      preferences[SERVER_ENDPOINT_KEY] = endpoint
    }
  }

  /** Returns the URL of the server host (protocol and host). */
  private fun hostUrl(preferences: Preferences): String {
    val protocol = preferences[SERVER_PROTOCOL_KEY] ?: DEFAULT_SERVER_PROTOCOL
    val host = preferences[SERVER_HOST_KEY] ?: DEFAULT_SERVER_HOST
    return "$protocol://$host"
  }

  private companion object {
    val SERVER_HOST_KEY = stringPreferencesKey("server_host")
    val SERVER_PORT_KEY = intPreferencesKey("server_port")
    val SERVER_PROTOCOL_KEY = stringPreferencesKey("server_protocol")
    val SERVER_ENDPOINT_KEY = stringPreferencesKey("server_endpoint")
    val SERVER_BASIC_AUTH_KEY = stringPreferencesKey("server_basic_auth")

    const val DEFAULT_SERVER_HOST = "82.96.147.205"
    const val DEFAULT_SERVER_PORT = 42080
    const val DEFAULT_SERVER_PROTOCOL = "http"
    const val DEFAULT_SERVER_ENDPOINT = "/card/remote-plugin"
  }
}
