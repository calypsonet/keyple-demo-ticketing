/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Display duration of a successful result before the automatic return. */
internal const val SUCCESS_RETURN_DELAY_MS = 5000L

/** Display duration of an error before the automatic return, longer to read the message. */
internal const val ERROR_RETURN_DELAY_MS = 8000L

/**
 * Automatic return to the previous screen after a result.
 *
 * @property delayMs The display duration of the result, in milliseconds.
 * @property color The color of the bar showing the remaining time.
 */
data class AutoReturn(val delayMs: Long, val color: Color)

/**
 * Calls [onReturn] after [delayMs], the remaining time being shown by a bar emptied during the
 * delay. The automatic return is abandoned when the bar leaves the screen (e.g. when the user goes
 * back before).
 */
@Composable
internal fun AutoReturnProgress(
    delayMs: Long,
    color: Color,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
  val remaining = remember { Animatable(1f) }
  val currentOnReturn by rememberUpdatedState(onReturn)
  LaunchedEffect(Unit) {
    // The bar is only a display: the return does not depend on the animation duration, which the
    // system may shorten or disable
    launch {
      remaining.animateTo(
          0f, animationSpec = tween(durationMillis = delayMs.toInt(), easing = LinearEasing))
    }
    delay(delayMs)
    currentOnReturn()
  }
  LinearProgressIndicator(
      progress = { remaining.value },
      modifier = modifier.fillMaxWidth().height(6.dp),
      color = color,
      trackColor = color.copy(alpha = 0.3f),
      gapSize = 0.dp,
      drawStopIndicator = {})
}
