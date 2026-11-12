/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.sales.data.platform

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import io.github.aakira.napier.Napier
import org.calypsonet.keyple.demo.sales.shared.R

actual class PlatformBuzzer(private val context: Context) {
  val vibrator = context.getSystemService(Vibrator::class.java)

  // Player of the sound being played, kept until the end of the sound: a player released (or
  // garbage collected) before stops its sound
  private var player: MediaPlayer? = null

  actual fun vibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }
  }

  /**
   * Plays the sound of the Android client (`reading_sound`), as it does: on the media stream, at
   * the volume set by the user for this stream.
   */
  @Synchronized
  actual fun beep() {
    player?.release()
    player =
        MediaPlayer.create(context, R.raw.reading_sound)?.apply {
          setOnCompletionListener { completed -> release(completed) }
          start()
        }
    if (player == null) {
      Napier.w("Sound not played [cause=media player not created]")
    }
  }

  /** Releases the player of a sound once it is over. */
  @Synchronized
  private fun release(completed: MediaPlayer) {
    completed.release()
    if (player === completed) {
      player = null
    }
  }
}
