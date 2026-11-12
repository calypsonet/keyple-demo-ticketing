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
package org.calypsonet.keyple.demo.sales.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.success.PlatformAnimation
import org.calypsonet.keyple.demo.sales.ui.theme.green
import org.calypsonet.keyple.demo.sales.ui.theme.red
import org.calypsonet.keyple.demo.sales.ui.theme.white
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Kind of result of a card operation, with the color and the animation of the Android client. */
enum class ResultKind(val color: Color, val animationFileName: String, val delayMs: Long) {
  SUCCESS(green, "anim_tick_white.json", SUCCESS_RETURN_DELAY_MS),
  ERROR(red, "anim_error_white.json", ERROR_RETURN_DELAY_MS),
}

// Width of the animation and of the message, as in the Android client (guidelines at 34 % and 66 %
// of the screen for the animation, at 15 % and 85 % for the message)
private const val MESSAGE_WIDTH_FRACTION = 0.7f
private const val ANIMATION_WIDTH_FRACTION = 0.32f / MESSAGE_WIDTH_FRACTION

/**
 * Result of a card operation (loading, personalization), laid out as in the Android client: the
 * whole screen takes the color of the result, the top bar included, then the screen returns
 * automatically to [onReturn] (also called by the back arrow).
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ResultScreen(
    navController: NavController,
    appState: AppState,
    kind: ResultKind,
    message: String,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(
        Res.readBytes("files/${kind.animationFileName}").decodeToString())
  }
  val progress by animateLottieCompositionAsState(composition)

  // The color of the result extends behind the system bars
  Scaffold(modifier = modifier, containerColor = kind.color) { innerPadding ->
    // The insets applied here are consumed, so that the top bar does not apply them again
    Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).fillMaxSize()) {
      Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AppTopBar(
            navController = navController,
            appState = appState,
            onBack = onReturn,
            onColoredBackground = true)

        // As in the Android client, the animation and the message are placed in the upper part of
        // the screen
        Column(
            modifier =
                Modifier.weight(1f).fillMaxWidth(MESSAGE_WIDTH_FRACTION).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(modifier = Modifier.weight(1f))
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlatformAnimation(
                message,
                white,
                composition,
                progress,
                infinite = false,
                animationModifier =
                    Modifier.weight(1f, fill = false)
                        .fillMaxWidth(ANIMATION_WIDTH_FRACTION)
                        .aspectRatio(1f))
          }
          Spacer(modifier = Modifier.weight(2f))
        }
      }
      AutoReturnProgress(
          delayMs = kind.delayMs,
          color = white,
          onReturn = onReturn,
          modifier = Modifier.align(Alignment.BottomCenter))
    }
  }
}
