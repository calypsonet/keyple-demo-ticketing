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
package org.calypsonet.keyple.demo.sales.ui.load

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.PresentCardAnimation
import org.calypsonet.keyple.demo.sales.ui.components.ResultKind
import org.calypsonet.keyple.demo.sales.ui.components.ResultScreen
import org.calypsonet.keyple.demo.sales.ui.components.ScanCardAnimation
import org.calypsonet.keyple.demo.sales.ui.components.ScanScreen
import org.calypsonet.keyple.demo.sales.ui.nav.Home
import org.calypsonet.keyple.demo.sales.ui.nav.Success

@Composable
fun LoadContractScreen(
    navController: NavController,
    viewModel: LoadContractScreenViewModel,
    modifier: Modifier = Modifier,
    appState: AppState
) {
  val state = viewModel.state.collectAsState()

  LoadContractScreen(
      navController = navController, state = state.value, modifier = modifier, appState = appState)
}

@Composable
internal fun LoadContractScreen(
    navController: NavController,
    state: LoadContractScreenState,
    modifier: Modifier = Modifier,
    appState: AppState
) {
  val backToHome = { navController.navigate(Home) { popUpTo(Home) { inclusive = true } } }

  // As in the Android client, a failure of the loading is a result screen (red)
  if (state is LoadContractScreenState.DisplayError) {
    ResultScreen(
        navController = navController,
        appState = appState,
        kind = ResultKind.ERROR,
        message = state.message,
        onReturn = backToHome,
        modifier = modifier)
    return
  }

  ScanScreen(
      navController = navController,
      modifier = modifier,
      appState = appState,
      onBack = backToHome) {
        when (state) {
          LoadContractScreenState.WaitForCard -> {
            PresentCardAnimation("Please present the contactless card to be loaded")
          }
          LoadContractScreenState.WritingToCard -> {
            ScanCardAnimation("Loading in progress")
          }
          LoadContractScreenState.DisplaySuccess -> {
            navController.navigate(Success) { popUpTo(Home) }
          }
          is LoadContractScreenState.DisplayError -> {}
        }
      }
}
