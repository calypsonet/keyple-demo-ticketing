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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_calypso
import org.calypsonet.keyple.composeapp.generated.resources.keyple_background
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.success.PlatformAnimation
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.LocalAppDimens
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.orange
import org.calypsonet.keyple.demo.sales.ui.theme.red
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.vectorResource

// Width of the messages: slightly narrower than in the Android client (guidelines at 15 % and 85 %
// of the screen), so that the messages remain clear of the triangles on the sides of the background
private const val MESSAGE_WIDTH_FRACTION = 0.64f

/**
 * Screen of a card operation (reading, loading, personalization), laid out as in the Android
 * client: top bar, subtitle, content (animation and messages) and Calypso logo.
 *
 * The content takes the height left by the other elements: its animation shrinks when the screen is
 * too small, so that the messages never overlap the logo.
 */
@Composable
fun ScanScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    appState: AppState,
    onBack: () -> Unit = { navController.popBackStack() },
    autoReturn: AutoReturn? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
  val dimens = LocalAppDimens.current
  Scaffold(
      modifier = modifier,
  ) { innerPadding ->
    // As in the Android client, the background extends behind the system bars
    KeypleBackground {
      // The insets applied here are consumed, so that the top bar does not apply them again
      Box(
          modifier =
              Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              AppTopBar(
                  navController = navController,
                  appState = appState,
                  onBack = onBack,
              )

              Text(
                  text = "Open Source API for Smart Ticketing",
                  modifier = Modifier.width(dimens.homeTextWidth),
                  style = AppTextStyles.mediumRegular,
                  color = blue,
                  textAlign = TextAlign.Center,
              )

              // As in the Android client, the content is placed in the upper part of the space
              // left (the space below it is twice the space above it). The content itself is not
              // weighted: its animation takes its full size, unless the space left is too small.
              Column(
                  modifier =
                      Modifier.weight(1f)
                          .fillMaxWidth(MESSAGE_WIDTH_FRACTION)
                          .padding(vertical = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content,
                )
                Spacer(modifier = Modifier.weight(2f))
              }

              Image(
                  imageVector = vectorResource(Res.drawable.ic_logo_calypso),
                  contentDescription = "Calypso logo",
                  modifier = Modifier.width(dimens.calypsoLogoWidth).padding(bottom = 4.dp))
            }
            // Automatic return after a result: same action as the back arrow
            autoReturn?.let {
              AutoReturnProgress(
                  delayMs = it.delayMs,
                  color = it.color,
                  onReturn = onBack,
                  modifier = Modifier.align(Alignment.BottomCenter))
            }
          }
    }
  }
}

/**
 * Background of the home screen and of the card operation screens, with the triangles of the
 * Android client.
 */
@Composable
internal fun KeypleBackground(content: @Composable BoxScope.() -> Unit) {
  Box(modifier = Modifier.fillMaxSize()) {
    Image(
        imageVector = vectorResource(Res.drawable.keyple_background),
        contentDescription = "Keyple app background",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.matchParentSize())
    content()
  }
}

/**
 * Size of the animations of the card operations: that of the Android client, reduced when the
 * height left to the content is too small.
 */
@Composable
internal fun ColumnScope.scanAnimationModifier(): Modifier {
  val size = LocalAppDimens.current.readingAnimationSize
  return Modifier.weight(1f, fill = false).sizeIn(maxWidth = size, maxHeight = size).aspectRatio(1f)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ColumnScope.PresentCardAnimation(message: String, warning: String? = null) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/anim_card_scan.json").decodeToString())
  }
  val progress by animateLottieCompositionAsState(composition)

  PlatformAnimation(
      message, blue, composition, progress, animationModifier = scanAnimationModifier())
  // Warning on the consequences of the operation (personalization: the card is reset), as in the
  // Android client: "Warning" title in bold, then the message
  warning?.let {
    Text(
        text = "Warning",
        modifier = Modifier.fillMaxWidth().padding(top = LocalAppDimens.current.warningTopMargin),
        style = AppTextStyles.mediumBold,
        color = orange,
        textAlign = TextAlign.Center,
    )
    Text(
        text = it,
        modifier = Modifier.fillMaxWidth(),
        style = AppTextStyles.mediumRegular,
        color = orange,
        textAlign = TextAlign.Center,
    )
  }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ColumnScope.ScanCardAnimation(message: String) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/anim_loading.json").decodeToString())
  }
  val progress by
      animateLottieCompositionAsState(composition, iterations = Compottie.IterateForever)

  PlatformAnimation(
      message, blue, composition, progress, animationModifier = scanAnimationModifier())
}

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ColumnScope.ReadingError(errorMessage: String) {
  val composition by rememberLottieComposition {
    LottieCompositionSpec.JsonString(Res.readBytes("files/anim_error.json").decodeToString())
  }
  val progress by
      animateLottieCompositionAsState(
          composition,
      )

  PlatformAnimation(
      errorMessage,
      red,
      composition,
      progress,
      infinite = false,
      animationModifier = scanAnimationModifier())
}
