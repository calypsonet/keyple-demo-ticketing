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
package org.calypsonet.keyple.demo.validation.ui.activities.terminalselection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.validation.BuildConfig
import org.calypsonet.keyple.demo.validation.R
import org.calypsonet.keyple.demo.validation.databinding.ActivityTerminalSelectionBinding
import org.calypsonet.keyple.demo.validation.domain.model.TerminalType
import org.calypsonet.keyple.demo.validation.ui.activities.BaseActivity
import org.calypsonet.keyple.demo.validation.ui.activities.SettingsActivity
import org.calypsonet.keyple.plugin.arrive.ArriveConstants
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants

@AndroidEntryPoint
class TerminalSelectionActivity : BaseActivity() {

  private lateinit var activityTerminalSelectionBinding: ActivityTerminalSelectionBinding

  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    activityTerminalSelectionBinding = ActivityTerminalSelectionBinding.inflate(layoutInflater)
    setContentView(activityTerminalSelectionBinding.root)

    // Bluebird
    if (BluebirdConstants.PLUGIN_NAME.contains(MOCK_PLUGIN_MARKER)) {
      activityTerminalSelectionBinding.bluebirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      activityTerminalSelectionBinding.bluebirdBtn.setOnClickListener {
        appSettings.terminalType = TerminalType.BLUEBIRD
        val permissions = mutableListOf("com.bluebird.permission.SAM_DEVICE_ACCESS")
        // Storage permission refused without prompt since Android 13 (declared up to Android 12)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
          permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val granted = PermissionHelper.requestMissingPermissions(this, permissions.toTypedArray())
        if (granted) {
          startActivity(Intent(this, SettingsActivity::class.java))
          finish()
        }
      }
    }

    // Famoco
    activityTerminalSelectionBinding.famocoBtn.setOnClickListener {
      appSettings.terminalType = TerminalType.FAMOCO
      startActivity(Intent(this, SettingsActivity::class.java))
      finish()
    }

    // Arrive
    if (ArriveConstants.PLUGIN_NAME.contains(MOCK_PLUGIN_MARKER) || !BuildConfig.HAS_ARRIVE_SDK) {
      activityTerminalSelectionBinding.arriveBtn.setBackgroundColor(Color.GRAY)
    } else {
      activityTerminalSelectionBinding.arriveBtn.setOnClickListener {
        appSettings.terminalType = TerminalType.ARRIVE
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      }
    }
    activityTerminalSelectionBinding.appVersion.text =
        getString(R.string.version, BuildConfig.VERSION_NAME)
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    when (requestCode) {
      PermissionHelper.PERMISSIONS_REQUEST_CODE -> {
        if (grantResults.isNotEmpty()) {
          for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
              PermissionDeniedDialog().apply {
                show(supportFragmentManager, PermissionDeniedDialog::class.java.simpleName)
              }
              return
            }
          }
          startActivity(Intent(applicationContext, SettingsActivity::class.java))
          finish()
        } else {
          PermissionDeniedDialog().apply {
            show(supportFragmentManager, PermissionDeniedDialog::class.java.simpleName)
          }
        }
        return
      }
      // Add other 'when' lines to check for other
      // permissions this app might request.
      else -> {
        // Ignore all other requests.
      }
    }
  }

  private companion object {
    /** Part of the name of the simulated plugins (mock libraries of the proprietary plugins). */
    const val MOCK_PLUGIN_MARKER = "Mock"
  }
}
