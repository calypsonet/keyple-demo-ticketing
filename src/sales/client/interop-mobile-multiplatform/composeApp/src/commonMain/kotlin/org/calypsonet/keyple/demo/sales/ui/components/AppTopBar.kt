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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.ic_arrow_back
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_keyple
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_white
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.theme.LocalAppDimens
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.green
import org.calypsonet.keyple.demo.sales.ui.theme.orange
import org.jetbrains.compose.resources.vectorResource

/** Size of the icons of the top bar (back arrow, settings), as in the Android client. */
val TOOLBAR_ICON_SIZE = 25.dp

// Height of the toolbar of the Android client (action bar), height of its logo, and text size of
// the name of the application below the logo
private val TOOLBAR_HEIGHT = 56.dp
private val TOOLBAR_LOGO_HEIGHT = 30.dp
private val TOOLBAR_APP_NAME_SIZE = 12.sp

/**
 * Top bar of the application, laid out as the toolbar of the Android client (`toolbar.xml`): back
 * arrow and server state at the start, logo in the center, actions (settings) at the end.
 *
 * The bar applies the insets of the status bar: a screen which displays it in its content consumes
 * the insets it applies itself.
 */
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    appState: AppState,
    actions: @Composable RowScope.() -> Unit = {},
    showBackArrow: Boolean = true,
    onBack: () -> Unit = { navController.popBackStack() },
    // On the colored background of a result, the Android client displays its white logo, without
    // the server state
    onColoredBackground: Boolean = false,
) {
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets.statusBars)
              .padding(top = LocalAppDimens.current.toolbarMarginTop)
              .height(TOOLBAR_HEIGHT)) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          if (showBackArrow) {
            IconButton(onClick = { onBack() }) {
              Icon(
                  imageVector = vectorResource(Res.drawable.ic_arrow_back),
                  contentDescription = "Back",
                  modifier = Modifier.size(TOOLBAR_ICON_SIZE),
                  tint = darkBlue,
              )
            }
          }
          if (!onColoredBackground) {
            ServerStateDot(
                state = appState,
                modifier = Modifier.padding(start = if (showBackArrow) 0.dp else 12.dp))
          }
        }

        val logo =
            vectorResource(
                if (onColoredBackground) Res.drawable.ic_logo_white
                else Res.drawable.ic_logo_keyple)
        // Keyple logo, with the name of the application below it, as on the launcher icon and in
        // the toolbar of the Android client
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Image(
                  imageVector = logo,
                  contentDescription = "Keyple logo",
                  modifier =
                      Modifier.height(TOOLBAR_LOGO_HEIGHT)
                          .aspectRatio(logo.defaultWidth / logo.defaultHeight),
              )
              AppName(onColoredBackground)
            }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
      }
}

/**
 * State of the server, with the colors of the Android client: green when the server is ready,
 * orange otherwise. When the server is ready, the dot pulses from time to time.
 */
@Composable
fun ServerStateDot(state: AppState, modifier: Modifier = Modifier) {
  val dotScale = remember { Animatable(1f) }
  val waves = remember { mutableStateListOf<Wave>() }

  val dotColor = if (state.isServerReady) green else orange

  LaunchedEffect(state.isServerReady) {
    if (state.isServerReady) {
      while (isActive) {
        // Animate the dot scale
        dotScale.animateTo(
            targetValue = 0.8f, animationSpec = tween(durationMillis = 200, easing = LinearEasing))
        dotScale.animateTo(
            targetValue = 1f, animationSpec = tween(durationMillis = 200, easing = LinearEasing))

        // Create the waves
        waves.add(Wave())

        delay(5000)
      }
    } else {
      // reset the animation to avoid residual effect
      dotScale.snapTo(1f)
      waves.clear()
    }
  }

  // Manage the waves and their animations
  LaunchedEffect(waves.size) {
    if (waves.isNotEmpty()) {
      waves.first().startAnimation()
      // Remove the wave after the animation.
      delay(Wave.ANIMATION_DURATION_MS)
      waves.removeAt(0)
    }
  }

  // Dot of the size of the Android client (12 dp), the waves being drawn around it without
  // changing its size
  Box(
      modifier =
          modifier
              .size(SERVER_STATE_DOT_SIZE)
              .drawBehind {
                waves.forEach { wave ->
                  drawCircle(
                      color = green,
                      radius = wave.radius.value.dp.toPx(),
                      center = Offset(size.width / 2, size.height / 2),
                      alpha = wave.alpha.value,
                      style = Stroke(width = 2.dp.toPx()))
                }
              }
              .scale(dotScale.value)
              .clip(CircleShape)
              .background(dotColor))
}

private val SERVER_STATE_DOT_SIZE = 12.dp

class Wave {
  val radius = Animatable(0f)
  val alpha = Animatable(0.2f)

  suspend fun startAnimation() {
    radius.animateTo(
        targetValue = 15f,
        animationSpec =
            tween(durationMillis = ANIMATION_DURATION_MS.toInt(), easing = FastOutSlowInEasing))
    alpha.animateTo(
        targetValue = 0f,
        animationSpec =
            tween(durationMillis = ANIMATION_DURATION_MS.toInt(), easing = LinearEasing))
  }

  companion object {
    const val ANIMATION_DURATION_MS = 200L
  }
}

/**
 * Name of the application, as in the Android client: initial in the color of the "y" of the logo
 * (blue, or white on a colored background where the logo is the white one), then the other letters
 * in black; "Interop" smaller and lighter, as a qualifier of "Sales".
 */
@Composable
private fun AppName(onColoredBackground: Boolean) {
  Text(
      text =
          buildAnnotatedString {
            withStyle(SpanStyle(color = if (onColoredBackground) Color.White else blue)) {
              append("S")
            }
            append("ales")
            withStyle(SpanStyle(fontSize = 0.78.em, color = Color.Black.copy(alpha = 0.65f))) {
              append(" Interop")
            }
          },
      color = Color.Black,
      fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = TOOLBAR_APP_NAME_SIZE,
      letterSpacing = 0.08.em,
      maxLines = 1)
}
