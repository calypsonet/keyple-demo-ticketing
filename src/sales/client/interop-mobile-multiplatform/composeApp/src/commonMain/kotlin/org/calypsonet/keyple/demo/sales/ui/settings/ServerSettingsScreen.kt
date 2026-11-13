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
package org.calypsonet.keyple.demo.sales.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar
import org.calypsonet.keyple.demo.sales.ui.components.MainButton
import org.calypsonet.keyple.demo.sales.ui.components.SecondaryButton
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.green
import org.calypsonet.keyple.demo.sales.ui.theme.grey
import org.calypsonet.keyple.demo.sales.ui.theme.lightBlue
import org.calypsonet.keyple.demo.sales.ui.theme.lineBlue
import org.calypsonet.keyple.demo.sales.ui.theme.red

// Margins of the settings of the Android client (block_space, settings_margin and
// settings_edit_padding)
private val SCREEN_PADDING = 10.dp
private val SETTINGS_MARGIN = 15.dp
private val EDIT_PADDING = 7.dp

@Composable
fun ServerSettingsScreen(
    navController: NavController,
    viewModel: ServerSettingsScreenViewModel,
    modifier: Modifier = Modifier,
    appState: AppState
) {
  val state = viewModel.state.collectAsState()

  ServerSettingsScreen(
      state = state.value,
      // As in the Android client, the settings screen is closed once the settings are saved
      onSaveServerSettings = {
        viewModel.saveServerSettings(onSaved = { navController.popBackStack() })
      },
      onTestServerReadiness = viewModel::testServerReadiness,
      navController = navController,
      modifier = modifier,
      appState = appState,
      onHostChanged = viewModel::onHostChanged,
      onPortChanged = viewModel::onPortChanged,
      onProtocolChanged = viewModel::onProtocolChanged)
}

@Composable
internal fun ServerSettingsScreen(
    state: ServerSettingsScreenState,
    onSaveServerSettings: () -> Unit,
    onTestServerReadiness: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    appState: AppState,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onProtocolChanged: (String) -> Unit,
) {
  Scaffold(
      topBar = { AppTopBar(navController = navController, appState = appState) },
      modifier = modifier,
  ) { innerPadding ->
    Column(Modifier.padding(innerPadding).fillMaxSize()) {
      // Settings laid out as in the Android client: label on the left, value on the right on a
      // light blue background, line below
      Column(Modifier.padding(horizontal = SCREEN_PADDING)) {
        SettingsField(
            label = "Server IP",
            value = state.host,
            onValueChange = onHostChanged,
            error =
                "Please set a valid IP address or host name"
                    .takeIf { state.isLoaded && !state.isHostValid })

        SettingsField(
            label = "Server port",
            value = state.port,
            onValueChange = onPortChanged,
            keyboardType = KeyboardType.Number,
            error =
                "Please set a port between 1 and 65535"
                    .takeIf { state.isLoaded && state.validatedPort == null })

        SettingsField(
            label = "Protocol",
            value = state.protocol,
            onValueChange = onProtocolChanged,
            error = "Please set http or https".takeIf { state.isLoaded && !state.isProtocolValid })

        // State of the server entered, right below the settings, as in the Android client: status
        // on the left (as the labels), test button on the right (as the values)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          ServerReadinessStatus(state.serverReadiness, Modifier.weight(1f))
          SecondaryButton(
              text = "Server state",
              onClick = onTestServerReadiness,
              enabled = state.isValid && state.serverReadiness != ServerReadiness.CHECKING)
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      MainButton(text = "Save", onClick = onSaveServerSettings, enabled = state.isValid)
    }
  }
}

/**
 * Setting of the server, as in the Android client: label (`smallRegularDarkBlue`) and value
 * (`smallMediumDarkBlue`, aligned on the right on a light blue background), then a line. An invalid
 * value is explained below it.
 */
@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = label, style = AppTextStyles.smallRegular, color = darkBlue)
      BasicTextField(
          value = value,
          onValueChange = onValueChange,
          singleLine = true,
          textStyle = AppTextStyles.smallMedium.copy(color = darkBlue, textAlign = TextAlign.End),
          keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
          cursorBrush = SolidColor(darkBlue),
          modifier =
              Modifier.weight(1f)
                  .padding(start = SETTINGS_MARGIN)
                  .background(lightBlue)
                  .padding(EDIT_PADDING)
                  .semantics { contentDescription = label },
      )
    }
    error?.let {
      Text(
          text = it,
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          style = AppTextStyles.smallerRegular,
          color = red,
          textAlign = TextAlign.End)
    }
    Box(
        modifier =
            Modifier.padding(vertical = SETTINGS_MARGIN)
                .fillMaxWidth()
                .height(1.dp)
                .background(lineBlue))
  }
}

/**
 * State of the server entered (not checked when null), as in the Android client: status light (or
 * progress indicator while the server is checked) and text, in the color of the state.
 */
@Composable
private fun ServerReadinessStatus(readiness: ServerReadiness?, modifier: Modifier = Modifier) {
  val (text, color) =
      when (readiness) {
        null -> "Not checked" to grey
        ServerReadiness.CHECKING -> "Checking…" to grey
        ServerReadiness.READY -> "Server ready" to green
        ServerReadiness.NOT_READY -> "Server not ready" to red
      }
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
      if (readiness == ServerReadiness.CHECKING) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = blue, strokeWidth = 2.dp)
      } else {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
      }
    }
    Text(
        text = text,
        modifier = Modifier.padding(start = 10.dp),
        style = AppTextStyles.smallRegular,
        color = color)
  }
}
