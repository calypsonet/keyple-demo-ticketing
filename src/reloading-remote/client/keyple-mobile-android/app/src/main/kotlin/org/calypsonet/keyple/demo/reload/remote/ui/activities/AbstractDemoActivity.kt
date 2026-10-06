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

import androidx.lifecycle.lifecycleScope
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.R
import org.calypsonet.keyple.demo.reload.remote.databinding.ToolbarBinding
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider
import org.calypsonet.keyple.demo.reload.remote.ui.events.ServerStatusEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/** Each Activity of the app should show status connexion result */
abstract class AbstractDemoActivity : DaggerAppCompatActivity() {

  @Inject lateinit var appSettings: AppSettingsRepository
  @Inject lateinit var serverStatusProvider: ServerStatusProvider
  protected lateinit var toolbarBinding: ToolbarBinding

  override fun onResume() {
    super.onResume()
    checkServerStatus()
  }

  override fun onStart() {
    super.onStart()
    EventBus.getDefault().register(this)
  }

  override fun onStop() {
    super.onStop()
    EventBus.getDefault().unregister(this)
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onServerStatusEvent(serverStatusEvent: ServerStatusEvent) {
    appSettings.lastServerStatus = serverStatusEvent.isUp
    updateServerStatusIndicator()
  }

  private fun updateServerStatusIndicator() {
    if (appSettings.lastServerStatus)
        toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_success)
    else toolbarBinding.serverStatus.setImageResource(R.drawable.ic_connection_wait)
  }

  private fun checkServerStatus() {
    val serverConfig = appSettings.serverConfig
    lifecycleScope.launch(Dispatchers.IO) {
      val isUp =
          try {
            serverStatusProvider.isSamReady(serverConfig)
          } catch (e: Exception) {
            false
          }
      EventBus.getDefault().post(ServerStatusEvent(isUp))
    }
  }
}
