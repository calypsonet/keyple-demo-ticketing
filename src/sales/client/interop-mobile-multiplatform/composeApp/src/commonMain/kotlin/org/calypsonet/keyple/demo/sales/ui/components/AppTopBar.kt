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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.ic_arrow_back
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_keyple
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_white
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.theme.LocalAppDimens
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.green
import org.calypsonet.keyple.demo.sales.ui.theme.orange
import org.jetbrains.compose.resources.vectorResource

/** Size of the icons of the top bar (back arrow, settings), as in the Android client. */
val TOOLBAR_ICON_SIZE = 25.dp

// Height of the toolbar of the Android client (action bar), and height of its logo once its
// vertical paddings are removed
private val TOOLBAR_HEIGHT = 56.dp
private val TOOLBAR_LOGO_HEIGHT = 32.dp

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
        Image(
            imageVector = logo,
            contentDescription = "Keyple logo",
            modifier =
                Modifier.align(Alignment.Center)
                    .height(TOOLBAR_LOGO_HEIGHT)
                    .aspectRatio(logo.defaultWidth / logo.defaultHeight),
        )

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
