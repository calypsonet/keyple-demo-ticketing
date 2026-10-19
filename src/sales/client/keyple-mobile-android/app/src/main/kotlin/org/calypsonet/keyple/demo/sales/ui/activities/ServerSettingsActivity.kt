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
package org.calypsonet.keyple.demo.sales.ui.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.sales.R
import org.calypsonet.keyple.demo.sales.databinding.ActivityServerSettingsBinding
import org.calypsonet.keyple.demo.sales.domain.model.ServerConfig
import timber.log.Timber

@AndroidEntryPoint
class ServerSettingsActivity : BaseActivity() {

  private lateinit var activityServerSettingsBinding: ActivityServerSettingsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityServerSettingsBinding = ActivityServerSettingsBinding.inflate(layoutInflater)
    toolbarBinding = activityServerSettingsBinding.appBarLayout
    setContentView(activityServerSettingsBinding.root)

    val serverConfig = appSettings.serverConfig
    activityServerSettingsBinding.serverIpEdit.text.append(serverConfig.ip)
    activityServerSettingsBinding.serverPortEdit.text.append(serverConfig.port.toString())
    activityServerSettingsBinding.serverProtocolEdit.text.append(serverConfig.protocol)

    // The new server is used from the next request: no restart of the application is needed
    activityServerSettingsBinding.save.setOnClickListener {
      readServerConfig()?.let {
        appSettings.serverConfig = it
        finish()
      }
    }

    activityServerSettingsBinding.serverReadinessBtn.setOnClickListener {
      readServerConfig()?.let { testServerReadiness(it) }
    }
  }

  /** Tests whether the server entered by the user is ready (reachable, with its SAM available). */
  private fun testServerReadiness(serverConfig: ServerConfig) {
    Timber.i("Checking the server [url=${serverConfig.url}]")
    activityServerSettingsBinding.serverReadinessProgressBar.visibility = View.VISIBLE
    activityServerSettingsBinding.serverReadinessText.visibility = View.INVISIBLE
    lifecycleScope.launch(Dispatchers.Main) {
      val isServerReady =
          withContext(Dispatchers.IO) {
            try {
              val isServerReady = serverReadinessChecker.isServerReady(serverConfig)
              if (!isServerReady) {
                Timber.w("Server not ready [url=${serverConfig.url}, cause=SAM not available]")
              }
              isServerReady
            } catch (e: Exception) {
              Timber.w("Server not ready [url=${serverConfig.url}, cause=${e.message}]")
              false
            }
          }
      activityServerSettingsBinding.serverReadinessProgressBar.visibility = View.INVISIBLE
      activityServerSettingsBinding.serverReadinessText.visibility = View.VISIBLE
      activityServerSettingsBinding.serverReadinessText.text =
          getString(if (isServerReady) R.string.server_ready else R.string.server_not_ready)
    }
  }

  /** Returns the server configuration entered by the user, or null if an entry is not valid. */
  private fun readServerConfig(): ServerConfig? {
    val ip = activityServerSettingsBinding.serverIpEdit.text.toString()
    if (!isIpv4Address(ip)) {
      activityServerSettingsBinding.serverIpEdit.error = getString(R.string.invalid_server_ip)
      return null
    }
    val port = activityServerSettingsBinding.serverPortEdit.text.toString().toIntOrNull()
    if (port == null) {
      activityServerSettingsBinding.serverPortEdit.error = getString(R.string.invalid_server_port)
      return null
    }
    val protocol = activityServerSettingsBinding.serverProtocolEdit.text.toString()
    if (protocol !in arrayOf("http://", "https://")) {
      activityServerSettingsBinding.serverProtocolEdit.error =
          getString(R.string.invalid_server_protocol)
      return null
    }
    return ServerConfig(protocol, ip, port)
  }

  /** Returns true if [ip] is an IPv4 address: four numbers from 0 to 255 separated by dots. */
  private fun isIpv4Address(ip: String): Boolean {
    val parts = ip.split('.')
    return parts.size == 4 &&
        parts.all { part ->
          part.length in 1..3 && part.all { it in '0'..'9' } && part.toInt() <= 255
        }
  }
}
