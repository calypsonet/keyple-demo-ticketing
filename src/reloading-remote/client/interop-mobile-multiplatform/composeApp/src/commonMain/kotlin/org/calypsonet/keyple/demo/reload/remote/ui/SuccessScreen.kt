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
package org.calypsonet.keyple.demo.reload.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.success_title_loaded
import org.calypsonet.keyple.demo.reload.remote.AppState
import org.calypsonet.keyple.demo.reload.remote.nav.Home
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun SuccessScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    appState: AppState,
) {
  // Back to the home screen, replacing the screens of the transaction (back arrow or automatic)
  val goHome = { navController.navigate(Home) { popUpTo(Home) { inclusive = true } } }

  Scaffold(
      topBar = {
        KeypleTopAppBar(navController = navController, appState = appState, onBack = goHome)
      },
      modifier = modifier,
  ) { innerPadding ->
    Box(Modifier.padding(innerPadding).fillMaxSize().background(green)) {
      Column(
          Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
      ) {
        DisplaySuccess("anim_tick_white.json", stringResource(Res.string.success_title_loaded))
      }
      AutoReturnProgress(
          delayMs = SUCCESS_RETURN_DELAY_MS,
          color = white,
          onReturn = goHome,
          modifier = Modifier.align(Alignment.BottomCenter))
    }
  }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun DisplaySuccess(animationFileName: String, message: String) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/$animationFileName").decodeToString())
  }
  val progress by
      animateLottieCompositionAsState(
          composition,
      )

  ScreenAnimByPlatform(message, white, composition, progress, infinite = false)
}

@Composable
expect fun ScreenAnimByPlatform(
    message: String,
    textColor: Color,
    composition: LottieComposition?,
    progress: Float,
    infinite: Boolean = true
)
