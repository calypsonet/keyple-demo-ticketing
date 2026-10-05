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
package org.calypsonet.keyple.demo.reload.remote.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ActivityServerSettingsBinding
import org.calypsonet.keyple.demo.reload.remote.di.scopes.ActivityScoped
import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig
import timber.log.Timber

@ActivityScoped
class ServerSettingsActivity : AbstractDemoActivity() {

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

    activityServerSettingsBinding.restart.setOnClickListener {
      readServerConfig()?.let {
        appSettings.serverConfig = it
        restartApp()
      }
    }

    activityServerSettingsBinding.pingBtn.setOnClickListener {
      readServerConfig()?.let { ping(it) }
    }
  }

  private fun ping(serverConfig: ServerConfig) {
    Timber.i("Ping server with URL: ${serverConfig.url}")
    activityServerSettingsBinding.pingProgressBar.visibility = View.VISIBLE
    activityServerSettingsBinding.pingResultText.visibility = View.INVISIBLE
    GlobalScope.launch(Dispatchers.Main) {
      val isReachable =
          withContext(Dispatchers.IO) {
            try {
              serverStatusProvider.isSamReady(serverConfig)
              true
            } catch (e: Exception) {
              Timber.e(e)
              false
            }
          }
      activityServerSettingsBinding.pingProgressBar.visibility = View.INVISIBLE
      activityServerSettingsBinding.pingResultText.visibility = View.VISIBLE
      activityServerSettingsBinding.pingResultText.text =
          getString(if (isReachable) R.string.ping_success else R.string.ping_failed)
    }
  }

  private fun restartApp() {
    startActivity(Intent(applicationContext, MainActivity::class.java))
    exitProcess(0)
  }

  /** Returns the server configuration entered by the user, or null if an entry is not valid. */
  private fun readServerConfig(): ServerConfig? {
    val ip = activityServerSettingsBinding.serverIpEdit.text.toString()
    if (ip.isBlank() || !Patterns.IP_ADDRESS.matcher(ip).matches()) {
      activityServerSettingsBinding.serverIpEdit.error = "Please set a valid IP"
      return null
    }
    val port = activityServerSettingsBinding.serverPortEdit.text.toString().toIntOrNull()
    if (port == null) {
      activityServerSettingsBinding.serverPortEdit.error = "Please set a valid Port"
      return null
    }
    val protocol = activityServerSettingsBinding.serverProtocolEdit.text.toString()
    if (protocol !in arrayOf("http://", "https://")) {
      activityServerSettingsBinding.serverProtocolEdit.error = "Please set a valid Protocol"
      return null
    }
    return ServerConfig(protocol, ip, port)
  }
}
