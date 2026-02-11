/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.parkeon.content.BindJoiner
import com.parkeon.sound.SoundManager
import com.parkeon.system.LedInterface
import java.io.File
import timber.log.Timber

/**
 * Manages UI feedback (LEDs, sounds) for Arrive terminals using the Parkeon SDK.
 *
 * Replaces the UI methods previously provided by the FlowBird plugin (FlowbirdUiManager), which
 * were removed in the Arrive plugin. Uses LedInterface and SoundManager from AndroidParkeonCommon.
 * Initialization is asynchronous via BindJoiner; display calls before init completes are silently
 * ignored.
 */
internal class ArriveUiManager(private val context: Context) {

  private var joiner: BindJoiner? = null
  private var ledInterface: LedInterface? = null
  private var soundManager: SoundManager? = null

  companion object {
    private const val SOUND_SUCCESS = "success.mp3"
    private const val SOUND_ERROR = "error.mp3"

    // Virtual LED IDs for Axio 4 (SysfsLeds driver) — front_led/extra_led are MultiColorLed
    private val LEDS_SUCCESS = listOf("validation_ok")
    private val LEDS_FAILED = listOf("validation_ko")
    private val LEDS_WAITING = listOf("hunting")
    private val LEDS_OFF = listOf("none")
  }

  fun init(onReady: () -> Unit = {}) {
    val services =
        mapOf(
            "leds" to Intent(com.parkeon.content.Intent.ACTION_LEDS),
            "sound" to Intent(com.parkeon.content.Intent.ACTION_SOUND_SERVICE))

    joiner =
        BindJoiner(
            context,
            services,
            object : BindJoiner.Listener {
              override fun onJoined(initDone: Boolean) {
                if (!initDone) {
                  Timber.e("ArriveUiManager: services binding failed (initDone=false)")
                  return
                }
                Timber.i("ArriveUiManager: services bound successfully")
                ledInterface = LedInterface.Stub.asInterface(joiner?.getService("leds"))
                soundManager = SoundManager.Stub.asInterface(joiner?.getService("sound"))
                deploySoundFiles()
                onReady()
              }

              override fun onBindLost(intent: Intent) {
                Timber.w("ArriveUiManager: bind lost for ${intent.action}")
                ledInterface = null
                soundManager = null
              }
            })
    joiner?.bind()
  }

  /** Copies sound files from assets to external files dir so SoundManager can access them. */
  private fun deploySoundFiles() {
    val soundDir =
        (context.getExternalFilesDir("sounds") ?: File(context.filesDir, "sounds")).also {
          it.mkdirs()
        }
    val files = listOf(SOUND_SUCCESS, SOUND_ERROR)
    val deployedUris = mutableListOf<Uri>()
    for (name in files) {
      val dest = File(soundDir, name)
      try {
        context.assets.open("default/media/$name").use { input ->
          dest.outputStream().use { output -> input.copyTo(output) }
        }
        deployedUris.add(Uri.fromFile(dest))
      } catch (e: Exception) {
        Timber.e(e, "ArriveUiManager: failed to deploy sound file '$name'")
      }
    }
    if (deployedUris.isNotEmpty()) {
      try {
        soundManager?.loadFiles(deployedUris)
      } catch (e: Exception) {
        Timber.e(e, "ArriveUiManager: failed to load sound files")
      }
    }
  }

  private fun setLeds(leds: List<String>) {
    try {
      ledInterface?.set(leds)
    } catch (e: Exception) {
      Timber.e(e, "ArriveUiManager: failed to set LEDs $leds")
    }
  }

  private fun playSound(filename: String) {
    try {
      soundManager?.startDefaultPlayingFile(filename)
    } catch (e: Exception) {
      Timber.e(e, "ArriveUiManager: failed to play sound '$filename'")
    }
  }

  fun displayResultSuccess() {
    setLeds(LEDS_SUCCESS)
    playSound(SOUND_SUCCESS)
  }

  fun displayResultFailed() {
    setLeds(LEDS_FAILED)
    playSound(SOUND_ERROR)
  }

  fun displayWaiting() {
    setLeds(LEDS_WAITING)
  }

  fun displayHuntingNone() {
    setLeds(LEDS_OFF)
  }

  fun release() {
    try {
      setLeds(LEDS_OFF)
    } catch (e: Exception) {
      Timber.e(e, "ArriveUiManager: error during release")
    }
    joiner?.unbind()
    joiner = null
    ledInterface = null
    soundManager = null
  }
}
