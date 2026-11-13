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
package org.calypsonet.keyple.demo.sales.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.logo_keyple
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.background
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/** Display duration of the splash screen, as in the Android client. */
private const val SPLASH_DELAY_MS = 6000L

// Width of the logo, as in the Android client (between the guidelines at 15 % and 85 % of the
// screen), with its proportions (2:1)
private const val LOGO_WIDTH_FRACTION = 0.7f
private const val LOGO_ASPECT_RATIO = 2f

/**
 * Splash screen, as in the Android client (`activity_splash`): animation of the triangles of the
 * background, logo of Keyple and its subtitle, then [onFinished] after [SPLASH_DELAY_MS].
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun SplashScreen(onFinished: () -> Unit) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/anim_splashscreen.json").decodeToString())
  }
  val currentOnFinished by rememberUpdatedState(onFinished)
  LaunchedEffect(Unit) {
    delay(SPLASH_DELAY_MS)
    currentOnFinished()
  }

  // As in the Android client, the content avoids the system bars, the background extends behind
  Box(modifier = Modifier.fillMaxSize().background(background)) {
    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
      Image(
          painter = rememberLottiePainter(composition = composition, iterations = 1),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
      )
      // As in the Android client, the logo is centered on the screen, its subtitle below it
      Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(Res.drawable.logo_keyple),
            contentDescription = "Keyple logo",
            modifier = Modifier.fillMaxWidth(LOGO_WIDTH_FRACTION).aspectRatio(LOGO_ASPECT_RATIO),
        )
        // As in the Android client, the subtitle takes the width of the screen, not that of the
        // logo
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
          Text(
              text = "Open Source API for Smart Ticketing",
              modifier = Modifier.fillMaxWidth(),
              style = AppTextStyles.mediumRegular,
              color = blue,
              textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}
