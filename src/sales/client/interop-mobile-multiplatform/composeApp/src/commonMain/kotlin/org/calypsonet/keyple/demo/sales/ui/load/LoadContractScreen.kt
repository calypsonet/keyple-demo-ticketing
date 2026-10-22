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
import org.calypsonet.keyple.demo.sales.ui.components.AutoReturn
import org.calypsonet.keyple.demo.sales.ui.components.ERROR_RETURN_DELAY_MS
import org.calypsonet.keyple.demo.sales.ui.components.PresentCardAnimation
import org.calypsonet.keyple.demo.sales.ui.components.ReadingError
import org.calypsonet.keyple.demo.sales.ui.components.ScanCardAnimation
import org.calypsonet.keyple.demo.sales.ui.components.ScanScreen
import org.calypsonet.keyple.demo.sales.ui.nav.Home
import org.calypsonet.keyple.demo.sales.ui.nav.Success
import org.calypsonet.keyple.demo.sales.ui.theme.red

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
  ScanScreen(
      navController = navController,
      modifier = modifier,
      appState = appState,
      onBack = { navController.navigate(Home) { popUpTo(Home) { inclusive = true } } },
      autoReturn =
          if (state is LoadContractScreenState.DisplayError) AutoReturn(ERROR_RETURN_DELAY_MS, red)
          else null) {
        when (state) {
          LoadContractScreenState.WaitForCard -> {
            PresentCardAnimation()
          }
          LoadContractScreenState.WritingToCard -> {
            ScanCardAnimation()
          }
          is LoadContractScreenState.DisplayError -> {
            ReadingError(state.message)
          }
          LoadContractScreenState.DisplaySuccess -> {
            navController.navigate(Success) { popUpTo(Home) }
          }
        }
      }
}
