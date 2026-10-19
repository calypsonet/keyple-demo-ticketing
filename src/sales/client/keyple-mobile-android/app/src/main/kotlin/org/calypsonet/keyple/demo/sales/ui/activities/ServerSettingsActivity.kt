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
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
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
    activityServerSettingsBinding.serverIpEdit.text.append(serverConfig.host)
    activityServerSettingsBinding.serverPortEdit.text.append(serverConfig.port.toString())
    activityServerSettingsBinding.serverProtocolEdit.text.append(serverConfig.protocol)

    // The entries are checked as they are typed, the buttons being enabled when they are all valid
    // (as in the multiplatform client)
    listOf(
            activityServerSettingsBinding.serverIpEdit,
            activityServerSettingsBinding.serverPortEdit,
            activityServerSettingsBinding.serverProtocolEdit)
        .forEach { edit ->
          edit.doAfterTextChanged {
            activityServerSettingsBinding.serverReadinessText.text = ""
            updateEntryChecks()
          }
        }
    updateEntryChecks()

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

  /**
   * Displays the error of each invalid entry, and enables the "Server state" and "Save" buttons
   * when all the entries are valid.
   */
  private fun updateEntryChecks() {
    val isHostValid =
        checkEntry(
            activityServerSettingsBinding.serverIpEdit, ::isHostValid, R.string.invalid_server_host)
    val isPortValid =
        checkEntry(
            activityServerSettingsBinding.serverPortEdit,
            { it.toIntOrNull() in 1..65535 },
            R.string.invalid_server_port)
    val isProtocolValid =
        checkEntry(
            activityServerSettingsBinding.serverProtocolEdit,
            { it in PROTOCOLS },
            R.string.invalid_server_protocol)
    val areEntriesValid = isHostValid && isPortValid && isProtocolValid
    listOf(activityServerSettingsBinding.save, activityServerSettingsBinding.serverReadinessBtn)
        .forEach { button ->
          button.isEnabled = areEntriesValid
          button.alpha = if (areEntriesValid) 1f else DISABLED_BUTTON_ALPHA
        }
  }

  /** Checks the entry of [edit] with [isValid], displaying [errorRes] when it is not valid. */
  private fun checkEntry(edit: EditText, isValid: (String) -> Boolean, errorRes: Int): Boolean {
    val valid = isValid(edit.text.toString().trim())
    edit.error = if (valid) null else getString(errorRes)
    return valid
  }

  /** Returns the server configuration entered by the user, or null if an entry is not valid. */
  private fun readServerConfig(): ServerConfig? {
    val host = activityServerSettingsBinding.serverIpEdit.text.toString().trim()
    val port =
        activityServerSettingsBinding.serverPortEdit.text.toString().trim().toIntOrNull()?.takeIf {
          it in 1..65535
        }
    val protocol = activityServerSettingsBinding.serverProtocolEdit.text.toString().trim()
    if (!isHostValid(host) || port == null || protocol !in PROTOCOLS) {
      return null
    }
    return ServerConfig(protocol, host, port)
  }

  /** Returns true if [host] is an IPv4 address or a host name, without protocol nor port. */
  private fun isHostValid(host: String): Boolean = isIpv4Address(host) || isHostName(host)

  /** Returns true if [host] is an IPv4 address: four numbers from 0 to 255 separated by dots. */
  private fun isIpv4Address(host: String): Boolean {
    val parts = host.split('.')
    return parts.size == 4 &&
        parts.all { part ->
          part.length in 1..3 && part.all { it in '0'..'9' } && part.toInt() <= 255
        }
  }

  /** Returns true if [host] is a host name: labels of letters, digits, and hyphens. */
  private fun isHostName(host: String): Boolean =
      host.isNotEmpty() &&
          host.any { it.isLetter() } &&
          host.split('.').all { label ->
            label.length in 1..63 &&
                label.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' } &&
                !label.startsWith("-") &&
                !label.endsWith("-")
          }

  private companion object {
    val PROTOCOLS = setOf("http", "https")
    const val DISABLED_BUTTON_ALPHA = 0.5f
  }
}
