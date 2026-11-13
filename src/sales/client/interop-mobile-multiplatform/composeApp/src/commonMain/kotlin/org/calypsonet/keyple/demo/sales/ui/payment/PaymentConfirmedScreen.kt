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
package org.calypsonet.keyple.demo.sales.ui.payment

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar
import org.calypsonet.keyple.demo.sales.ui.components.MainButton
import org.calypsonet.keyple.demo.sales.ui.success.PlatformAnimation
import org.calypsonet.keyple.demo.sales.ui.theme.green
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Widths of the message and of the animation, as in the Android client (guidelines at 30 % and
// 70 % of the screen for the message, at 34 % and 66 % for the animation)
private const val MESSAGE_WIDTH_FRACTION = 0.4f
private const val ANIMATION_WIDTH_FRACTION = 0.32f / MESSAGE_WIDTH_FRACTION

/**
 * Confirmation of the payment, as in the Android client (`activity_payment_confirmed`): green tick
 * and message, then the button starting the loading of the ticket ([onLoadTicket]). The back arrow
 * returns to the basket.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun PaymentConfirmedScreen(
    navController: NavController,
    appState: AppState,
    onLoadTicket: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/anim_tick.json").decodeToString())
  }
  val progress by animateLottieCompositionAsState(composition)

  Scaffold(
      topBar = { AppTopBar(navController = navController, appState = appState) },
      modifier = modifier,
  ) { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // As in the Android client, the animation and the message are placed in the upper part of the
      // space left by the button
      Column(
          modifier =
              Modifier.weight(1f).fillMaxWidth(MESSAGE_WIDTH_FRACTION).padding(vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          PlatformAnimation(
              "Payment confirmed",
              green,
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

      MainButton(text = "Load ticket", onClick = onLoadTicket)
    }
  }
}
