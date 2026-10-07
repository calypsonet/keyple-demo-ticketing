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

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider

/** Each Activity of the app should show status connexion result */
abstract class AbstractDemoActivity : AppCompatActivity() {

  @Inject lateinit var appSettings: AppSettingsRepository
  @Inject lateinit var serverStatusProvider: ServerStatusProvider
  protected lateinit var toolbarBinding: ToolbarBinding

  override fun onResume() {
    super.onResume()
    checkServerStatus()
  }

  private fun updateServerStatusIndicator() {
    if (appSettings.lastServerStatus)
        toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_success)
    else toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_wait)
  }

  /** Requests the server status in background, then updates the indicator in the main thread. */
  private fun checkServerStatus() {
    val serverConfig = appSettings.serverConfig
    lifecycleScope.launch {
      val isUp =
          withContext(Dispatchers.IO) {
            try {
              serverStatusProvider.isSamReady(serverConfig)
            } catch (e: Exception) {
              false
            }
          }
      appSettings.lastServerStatus = isUp
      updateServerStatusIndicator()
    }
  }
}
