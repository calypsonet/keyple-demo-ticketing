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
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMedium
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardMediumVisibility
import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository

/** Application settings persisted in the shared preferences. */
class SharedPreferencesAppSettingsRepository(private val preferences: SharedPreferences) :
    AppSettingsRepository {

  override var serverConfig: ServerConfig
    get() =
        ServerConfig(
            protocol =
                preferences.getString(SERVER_PROTOCOL_KEY, DEFAULT_PROTOCOL) ?: DEFAULT_PROTOCOL,
            ip = preferences.getString(SERVER_IP_KEY, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP,
            port = preferences.getInt(SERVER_PORT_KEY, DEFAULT_PORT))
    set(value) {
      preferences
          .edit()
          .putString(SERVER_PROTOCOL_KEY, value.protocol)
          .putString(SERVER_IP_KEY, value.ip)
          .putInt(SERVER_PORT_KEY, value.port)
          .apply()
    }

  override var cardMedium: CardMedium
    get() = CardMedium.fromName(preferences.getString(CARD_MEDIUM_KEY, "") ?: "")
    set(value) {
      preferences.edit().putString(CARD_MEDIUM_KEY, value.toString()).apply()
    }

  override var wasServerReady: Boolean
    get() = preferences.getBoolean(WAS_SERVER_READY_KEY, false)
    set(value) {
      preferences.edit().putBoolean(WAS_SERVER_READY_KEY, value).apply()
    }

  override fun getCardMediumVisibility(cardMedium: CardMedium): CardMediumVisibility {
    val defaultVisibility =
        if (cardMedium == CardMedium.CONTACTLESS_CARD) CardMediumVisibility.ENABLE
        else CardMediumVisibility.DISABLE
    val value = preferences.getString(visibilityKey(cardMedium), null) ?: return defaultVisibility
    return CardMediumVisibility.valueOf(value.uppercase(Locale.ROOT))
  }

  override fun setCardMediumVisibility(cardMedium: CardMedium, visibility: CardMediumVisibility) {
    preferences
        .edit()
        .putString(visibilityKey(cardMedium), visibility.name.lowercase(Locale.ROOT))
        .apply()
  }

  private fun visibilityKey(cardMedium: CardMedium): String =
      when (cardMedium) {
        CardMedium.CONTACTLESS_CARD -> CONTACTLESS_VISIBILITY_KEY
        CardMedium.SIM -> SIM_VISIBILITY_KEY
      }

  companion object {
    private const val SERVER_IP_KEY = "server_ip"
    private const val SERVER_PORT_KEY = "server_port"
    private const val SERVER_PROTOCOL_KEY = "server_protocol"
    private const val CARD_MEDIUM_KEY = "card_medium"
    private const val DEFAULT_SERVER_IP = "192.168.0.1"
    private const val DEFAULT_PORT = 8080
    private const val DEFAULT_PROTOCOL = "http://"
    private const val CONTACTLESS_VISIBILITY_KEY = "contactless_visibility"
    private const val SIM_VISIBILITY_KEY = "sim_visibility"
    private const val WAS_SERVER_READY_KEY = "was_server_ready"
  }
}
