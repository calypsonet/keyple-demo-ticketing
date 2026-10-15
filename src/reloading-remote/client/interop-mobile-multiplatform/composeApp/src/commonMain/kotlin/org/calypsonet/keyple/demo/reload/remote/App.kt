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
package org.calypsonet.keyple.demo.reload.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.calypsonet.keyple.demo.reload.remote.data.platform.Buzzer
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.data.service.ReadCardCache
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerReadinessChecker
import org.calypsonet.keyple.demo.reload.remote.ui.cardcontent.CardContentScreen
import org.calypsonet.keyple.demo.reload.remote.ui.cardcontent.CardContentScreenViewModel
import org.calypsonet.keyple.demo.reload.remote.ui.home.HomeScreen
import org.calypsonet.keyple.demo.reload.remote.ui.load.LoadContractScreen
import org.calypsonet.keyple.demo.reload.remote.ui.load.LoadContractScreenViewModel
import org.calypsonet.keyple.demo.reload.remote.ui.nav.CardContent
import org.calypsonet.keyple.demo.reload.remote.ui.nav.Home
import org.calypsonet.keyple.demo.reload.remote.ui.nav.LoadContract
import org.calypsonet.keyple.demo.reload.remote.ui.nav.PersonalizeCard
import org.calypsonet.keyple.demo.reload.remote.ui.nav.ReadCard
import org.calypsonet.keyple.demo.reload.remote.ui.nav.ServerSettings
import org.calypsonet.keyple.demo.reload.remote.ui.nav.SettingsMenu
import org.calypsonet.keyple.demo.reload.remote.ui.nav.Success
import org.calypsonet.keyple.demo.reload.remote.ui.personalize.PersonalizeCardScreen
import org.calypsonet.keyple.demo.reload.remote.ui.personalize.PersonalizeCardScreenViewModel
import org.calypsonet.keyple.demo.reload.remote.ui.read.ReadCardScreen
import org.calypsonet.keyple.demo.reload.remote.ui.read.ReadCardScreenViewModel
import org.calypsonet.keyple.demo.reload.remote.ui.settings.ServerSettingsScreen
import org.calypsonet.keyple.demo.reload.remote.ui.settings.ServerSettingsScreenViewModel
import org.calypsonet.keyple.demo.reload.remote.ui.settings.SettingsMenuScreen
import org.calypsonet.keyple.demo.reload.remote.ui.success.SuccessScreen
import org.calypsonet.keyple.demo.reload.remote.ui.theme.AppTheme

@Composable
@Preview
fun App(
    keypleInteropService: KeypleInteropService,
    readCardCache: ReadCardCache,
    serverConfigRepository: ServerConfigRepository,
    serverReadinessChecker: ServerReadinessChecker,
    buzzer: Buzzer
) {
  val appViewModel =
      viewModel<AppViewModel> {
        AppViewModel(
            keypleInteropService = keypleInteropService,
            serverReadinessChecker = serverReadinessChecker,
            serverConfigRepository = serverConfigRepository)
      }
  val serverSettingsViewModel =
      viewModel<ServerSettingsScreenViewModel> {
        ServerSettingsScreenViewModel(
            keypleInteropService = keypleInteropService,
            serverConfigRepository = serverConfigRepository)
      }

  val navController = rememberNavController()

  val appState = appViewModel.state.collectAsState()

  AppTheme {
    NavHost(navController = navController, startDestination = Home) {
      composable<Home> { HomeScreen(navController = navController, appState = appState.value) }

      composable<SettingsMenu> {
        SettingsMenuScreen(navController = navController, appState = appState.value)
      }

      composable<ReadCard> {
        ReadCardScreen(
            navController = navController,
            viewModel =
                viewModel<ReadCardScreenViewModel> {
                  ReadCardScreenViewModel(
                      keypleInteropService = keypleInteropService,
                      readCardCache = readCardCache,
                      buzzer = buzzer)
                },
            appState = appState.value)
      }

      composable<LoadContract> { backStackEntry ->
        val route = backStackEntry.toRoute<LoadContract>()

        LoadContractScreen(
            navController = navController,
            viewModel =
                viewModel<LoadContractScreenViewModel> {
                  LoadContractScreenViewModel(
                      keypleInteropService = keypleInteropService, buzzer = buzzer, route = route)
                },
            appState = appState.value)
      }

      composable<PersonalizeCard> {
        PersonalizeCardScreen(
            navController = navController,
            viewModel =
                viewModel<PersonalizeCardScreenViewModel> {
                  PersonalizeCardScreenViewModel(
                      keypleInteropService = keypleInteropService, buzzer = buzzer)
                },
            appState = appState.value)
      }

      composable<CardContent> {
        CardContentScreen(
            navController = navController,
            appState = appState.value,
            viewModel =
                viewModel<CardContentScreenViewModel> {
                  CardContentScreenViewModel(readCardCache = readCardCache)
                })
      }

      composable<Success> {
        SuccessScreen(navController = navController, appState = appState.value)
      }

      composable<ServerSettings> {
        ServerSettingsScreen(
            navController = navController,
            viewModel = serverSettingsViewModel,
            appState = appState.value)
      }
    }
  }
}
