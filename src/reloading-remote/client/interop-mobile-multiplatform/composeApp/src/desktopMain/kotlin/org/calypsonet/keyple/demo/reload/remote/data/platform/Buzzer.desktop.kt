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
package org.calypsonet.keyple.demo.reload.remote.data.platform

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Sample rate of the tones, in Hz. */
private const val SAMPLE_RATE = 8000f

actual class PlatformBuzzer {

  // The tones are played in background, a failing tone not preventing the next ones
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  actual fun vibrate() {
    // do nothing on desktop...
  }

  /** Plays a tone of the given frequency and duration, in background. */
  @Throws(LineUnavailableException::class)
  fun playTone(frequencyHz: Int, durationMs: Int, volume: Double = 1.0) {
    scope.launch {
      val sample = ByteArray(1)
      val audioFormat =
          AudioFormat(
              SAMPLE_RATE, // sampleRate
              8, // sampleSizeInBits
              1, // channels
              true, // signed
              false) // bigEndian
      val line = AudioSystem.getSourceDataLine(audioFormat)
      line.open(audioFormat)
      line.start()
      for (i in 0..<durationMs * 8) {
        val angle = i / (SAMPLE_RATE / frequencyHz) * 2.0 * Math.PI
        sample[0] = (sin(angle) * 127.0 * volume).toInt().toByte()
        line.write(sample, 0, 1)
      }
      line.drain()
      line.stop()
      line.close()
    }
  }

  actual fun beep() {
    playTone(1200, 100)
  }
}
