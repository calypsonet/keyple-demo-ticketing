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
package org.calypsonet.keyple.demo.control.ui.activities.deviceselection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.nfc.NfcManager
import android.os.Build
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.calypsonet.keyple.demo.control.BuildConfig
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ActivityDeviceSelectionBinding
import org.calypsonet.keyple.demo.control.domain.model.TerminalType
import org.calypsonet.keyple.demo.control.ui.activities.BaseActivity
import org.calypsonet.keyple.demo.control.ui.activities.SettingsActivity
import org.calypsonet.keyple.plugin.bluebird.BluebirdConstants

@AndroidEntryPoint
class DeviceSelectionActivity : BaseActivity() {

  private lateinit var activityDeviceSelectionBinding: ActivityDeviceSelectionBinding

  private val mock: String = "Mock"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityDeviceSelectionBinding = ActivityDeviceSelectionBinding.inflate(layoutInflater)
    setContentView(activityDeviceSelectionBinding.root)
    activityDeviceSelectionBinding.appVersion.text =
        getString(R.string.version, BuildConfig.VERSION_NAME)
    // Bluebird
    if (BluebirdConstants.PLUGIN_NAME.contains(mock)) {
      activityDeviceSelectionBinding.bluebirdBtn.setBackgroundColor(Color.GRAY)
    } else {
      activityDeviceSelectionBinding.bluebirdBtn.setOnClickListener {
        appSettings.terminalType = TerminalType.BLUEBIRD
        val permissions = mutableListOf("com.bluebird.permission.SAM_DEVICE_ACCESS")
        // Storage permission refused without prompt since Android 13 (declared up to Android 12)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
          permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val granted = PermissionHelper.checkPermission(this, permissions.toTypedArray())
        if (granted) {
          startActivity(Intent(this, SettingsActivity::class.java))
          finish()
        }
      }
    }
    // Famoco
    activityDeviceSelectionBinding.famocoBtn.setOnClickListener {
      appSettings.terminalType = TerminalType.FAMOCO
      startActivity(Intent(this, SettingsActivity::class.java))
      finish()
    }
    // Standard NFC terminal
    activityDeviceSelectionBinding.nfcTerminalBtn.setOnClickListener {
      val nfcManager = getSystemService(NFC_SERVICE) as NfcManager
      if (nfcManager.defaultAdapter?.isEnabled == true) {
        appSettings.terminalType = TerminalType.NFC_TERMINAL
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
      } else {
        EnableNfcDialog().apply {
          show(supportFragmentManager, EnableNfcDialog::class.java.simpleName)
        }
      }
    }
  }

  @SuppressLint("MissingSuperCall")
  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    when (requestCode) {
      PermissionHelper.MY_PERMISSIONS_REQUEST_ALL -> {
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
}
