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
package org.calypsonet.keyple.demo.sales.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** Display duration of a successful result before the automatic return. */
internal const val SUCCESS_RETURN_DELAY_MS = 5000L

/** Display duration of an error before the automatic return. */
internal const val ERROR_RETURN_DELAY_MS = 5000L

/**
 * Automatic return to the previous screen after a result.
 *
 * @property delayMs The display duration of the result, in milliseconds.
 * @property color The color of the bar showing the remaining time.
 */
data class AutoReturn(val delayMs: Long, val color: Color)

/**
 * Calls [onReturn] after [delayMs], the remaining time being shown by a bar emptied during the
 * delay. The automatic return is abandoned when the bar leaves the screen (for example, when the
 * user goes back before).
 */
/** Update period of the bar showing the remaining time, in milliseconds. */
private const val UPDATE_PERIOD_MS = 40L

@Composable
internal fun AutoReturnProgress(
    delayMs: Long,
    color: Color,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
  var remaining by remember { mutableFloatStateOf(1f) }
  val currentOnReturn by rememberUpdatedState(onReturn)
  LaunchedEffect(Unit) {
    // The bar is updated from the elapsed time, and not with an animation: the animations follow
    // the animation duration scale of the system, which some terminals disable (the bar would then
    // be empty from the start)
    val start = TimeSource.Monotonic.markNow()
    while (true) {
      val elapsedMs = start.elapsedNow().inWholeMilliseconds
      if (elapsedMs >= delayMs) break
      remaining = 1f - elapsedMs.toFloat() / delayMs
      delay(UPDATE_PERIOD_MS)
    }
    remaining = 0f
    currentOnReturn()
  }
  LinearProgressIndicator(
      progress = { remaining },
      modifier = modifier.fillMaxWidth().height(6.dp),
      color = color,
      trackColor = color.copy(alpha = 0.3f),
      gapSize = 0.dp,
      drawStopIndicator = {})
}
