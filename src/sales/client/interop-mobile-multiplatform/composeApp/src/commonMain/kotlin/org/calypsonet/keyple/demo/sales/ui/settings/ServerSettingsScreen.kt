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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar

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
      onProtocolChanged = viewModel::onProtocolChanged,
      onEndpointChanged = viewModel::onEndpointChanged)
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
    onEndpointChanged: (String) -> Unit
) {
  Scaffold(
      topBar = { AppTopBar(navController = navController, appState = appState) },
      modifier = modifier,
  ) { innerPadding ->
    Column(
        Modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
          OutlinedTextField(
              value = state.host,
              onValueChange = onHostChanged,
              singleLine = true,
              isError = state.isLoaded && !state.isHostValid,
              supportingText = {
                if (state.isLoaded && !state.isHostValid) {
                  Text("Please set a valid IP address or host name")
                }
              },
              label = { Text("Server IP") })

          OutlinedTextField(
              value = state.port,
              onValueChange = onPortChanged,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              isError = state.isLoaded && state.validatedPort == null,
              supportingText = {
                if (state.isLoaded && state.validatedPort == null) {
                  Text("Please set a port between 1 and 65535")
                }
              },
              label = { Text("Server port") })

          OutlinedTextField(
              value = state.protocol,
              onValueChange = onProtocolChanged,
              singleLine = true,
              isError = state.isLoaded && !state.isProtocolValid,
              supportingText = {
                if (state.isLoaded && !state.isProtocolValid) {
                  Text("Please set http or https")
                }
              },
              label = { Text("Protocol") })

          OutlinedTextField(
              value = state.endpoint,
              onValueChange = onEndpointChanged,
              singleLine = true,
              isError = state.isLoaded && !state.isEndpointValid,
              supportingText = {
                if (state.isLoaded && !state.isEndpointValid) {
                  Text("Please set a path starting with /")
                }
              },
              label = { Text("Endpoint") })

          Spacer(modifier = Modifier.weight(1f))

          // Test of the server entered, before saving it (as the "Server state" button of the
          // Android client)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onTestServerReadiness,
                enabled = state.isValid && state.serverReadiness != ServerReadiness.CHECKING) {
                  Text("Server state")
                }
            when (state.serverReadiness) {
              ServerReadiness.CHECKING ->
                  CircularProgressIndicator(modifier = Modifier.padding(start = 16.dp).size(24.dp))
              ServerReadiness.READY -> Text("Server ready", Modifier.padding(start = 16.dp))
              ServerReadiness.NOT_READY -> Text("Server not ready", Modifier.padding(start = 16.dp))
              null -> {}
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(onClick = onSaveServerSettings, enabled = state.isValid) { Text("Save") }

          Spacer(modifier = Modifier.height(16.dp))
        }
  }
}
