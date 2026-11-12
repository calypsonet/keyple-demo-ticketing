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
package org.calypsonet.keyple.demo.sales.ui.personalize

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

@Composable
fun PersonalizeCardScreen(
    navController: NavController,
    viewModel: PersonalizeCardScreenViewModel,
    modifier: Modifier = Modifier,
    appState: AppState
) {
  val state = viewModel.state.collectAsState()

  PersonalizeCardScreen(
      navController = navController, state = state.value, modifier = modifier, appState = appState)
}

@Composable
internal fun PersonalizeCardScreen(
    navController: NavController,
    state: PersonalizeCardScreenState,
    modifier: Modifier = Modifier,
    appState: AppState
) {
  val backToHome = { navController.navigate(Home) { popUpTo(Home) { inclusive = true } } }

  // As in the Android client, the result of the personalization is a result screen (green or red)
  when (state) {
    PersonalizeCardScreenState.DisplaySuccess -> {
      ResultScreen(
          navController = navController,
          appState = appState,
          kind = ResultKind.SUCCESS,
          message = "Card personalized",
          onReturn = backToHome,
          modifier = modifier)
      return
    }
    is PersonalizeCardScreenState.DisplayError -> {
      ResultScreen(
          navController = navController,
          appState = appState,
          kind = ResultKind.ERROR,
          message = state.message,
          onReturn = backToHome,
          modifier = modifier)
      return
    }
    else -> {}
  }

  ScanScreen(
      navController = navController,
      modifier = modifier,
      appState = appState,
      onBack = backToHome) {
        when (state) {
          PersonalizeCardScreenState.WaitForCard -> {
            PresentCardAnimation(
                "Please present the contactless card to be personalized",
                warning = "All the content of the card will be reset.")
          }
          PersonalizeCardScreenState.WritingToCard -> {
            ScanCardAnimation("Personalization in progress")
          }
          else -> {}
        }
      }
}
