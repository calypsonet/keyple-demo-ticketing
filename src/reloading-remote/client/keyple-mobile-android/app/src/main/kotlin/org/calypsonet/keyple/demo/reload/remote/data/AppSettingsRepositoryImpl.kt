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
package org.calypsonet.keyple.demo.reload.remote.data

import android.content.SharedPreferences
import java.util.Locale
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceType
import org.calypsonet.keyple.demo.reload.remote.domain.model.DeviceVisibility
import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository

/** Application settings persisted in the shared preferences. */
class AppSettingsRepositoryImpl(private val prefs: SharedPreferences) : AppSettingsRepository {

  override var serverConfig: ServerConfig
    get() =
        ServerConfig(
            protocol = prefs.getString(SERVER_PROTOCOL_KEY, DEFAULT_PROTOCOL) ?: DEFAULT_PROTOCOL,
            ip = prefs.getString(SERVER_IP_KEY, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP,
            port = prefs.getInt(SERVER_PORT_KEY, DEFAULT_PORT))
    set(value) {
      prefs
          .edit()
          .putString(SERVER_PROTOCOL_KEY, value.protocol)
          .putString(SERVER_IP_KEY, value.ip)
          .putInt(SERVER_PORT_KEY, value.port)
          .apply()
    }

  override var deviceType: DeviceType
    get() = DeviceType.fromName(prefs.getString(DEVICE_TYPE, "") ?: "")
    set(value) {
      prefs.edit().putString(DEVICE_TYPE, value.toString()).apply()
    }

  override var lastServerStatus: Boolean
    get() = prefs.getBoolean(SETTING_SERVER_LAST_STATUS_UP, false)
    set(value) {
      prefs.edit().putBoolean(SETTING_SERVER_LAST_STATUS_UP, value).apply()
    }

  override fun getDeviceVisibility(device: DeviceType): DeviceVisibility {
    val defaultVisibility =
        if (device == DeviceType.CONTACTLESS_CARD) DeviceVisibility.ENABLE
        else DeviceVisibility.DISABLE
    val value = prefs.getString(visibilityKey(device), null) ?: return defaultVisibility
    return DeviceVisibility.valueOf(value.uppercase(Locale.ROOT))
  }

  override fun setDeviceVisibility(device: DeviceType, visibility: DeviceVisibility) {
    prefs.edit().putString(visibilityKey(device), visibility.name.lowercase(Locale.ROOT)).apply()
  }

  private fun visibilityKey(device: DeviceType): String =
      when (device) {
        DeviceType.CONTACTLESS_CARD -> SETTING_CONTACTLESS_VISIBILITY
        DeviceType.SIM -> SETTING_SIM_VISIBILITY
        DeviceType.WEARABLE -> SETTING_WEARABLE_VISIBILITY
        DeviceType.EMBEDDED -> SETTING_EMBEDDED_VISIBILITY
      }

  companion object {
    private const val SERVER_IP_KEY = "server_ip_key"
    private const val SERVER_PORT_KEY = "server_port_key"
    private const val SERVER_PROTOCOL_KEY = "server_protocol_key"
    private const val DEVICE_TYPE = "device_type"
    private const val DEFAULT_SERVER_IP = "192.168.0.1"
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_PROTOCOL = "http://"
    private const val SETTING_CONTACTLESS_VISIBILITY = "setting_contactless_visibility"
    private const val SETTING_SIM_VISIBILITY = "setting_sim_visibility"
    private const val SETTING_WEARABLE_VISIBILITY = "setting_wearable_visibility"
    private const val SETTING_EMBEDDED_VISIBILITY = "setting_embedded_visibility"
    private const val SETTING_SERVER_LAST_STATUS_UP = "setting_server_last_status_up"
  }
}
