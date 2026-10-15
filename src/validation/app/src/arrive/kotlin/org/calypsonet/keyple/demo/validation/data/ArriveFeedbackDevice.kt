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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.parkeon.content.BindJoiner
import com.parkeon.sound.SoundManager
import com.parkeon.system.LedInterface
import java.io.File
import org.calypsonet.keyple.demo.validation.domain.spi.Logger

/**
 * User feedback (LEDs, sounds) of the Arrive terminals, using the Parkeon SDK.
 *
 * Replaces the UI methods previously provided by the FlowBird plugin (FlowbirdUserFeedback), which
 * were removed in the Arrive plugin. Uses LedInterface and SoundManager from AndroidParkeonCommon.
 * Initialization is asynchronous via BindJoiner; display calls before init completes are silently
 * ignored.
 */
internal class ArriveFeedbackDevice(private val context: Context, private val logger: Logger) :
    FeedbackDevice {

  private var joiner: BindJoiner? = null
  private var ledInterface: LedInterface? = null
  private var soundManager: SoundManager? = null

  companion object {
    private const val SOUND_SUCCESS = "success.mp3"
    private const val SOUND_FAILURE = "error.mp3"

    // Virtual LED IDs for Axio 4 (SysfsLeds driver) — front_led/extra_led are MultiColorLed
    private val LEDS_SUCCESS = listOf("validation_ok")
    private val LEDS_FAILURE = listOf("validation_ko")
    private val LEDS_WAITING = listOf("hunting")
    private val LEDS_OFF = listOf("none")
  }

  override fun init(onReady: () -> Unit) {
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
                  logger.e("Arrive services binding failed")
                  return
                }
                logger.i("Arrive services bound")
                ledInterface = LedInterface.Stub.asInterface(joiner?.getService("leds"))
                soundManager = SoundManager.Stub.asInterface(joiner?.getService("sound"))
                deploySoundFiles()
                onReady()
              }

              override fun onBindLost(intent: Intent) {
                logger.w("Arrive services binding lost [action=${intent.action}]")
                ledInterface = null
                soundManager = null
              }
            })
    joiner?.bind()
  }

  /** Copies the sound files from the assets to the external files directory (for SoundManager). */
  private fun deploySoundFiles() {
    val soundDirectory =
        (context.getExternalFilesDir("sounds") ?: File(context.filesDir, "sounds")).also {
          it.mkdirs()
        }
    val fileNames = listOf(SOUND_SUCCESS, SOUND_FAILURE)
    val deployedUris = mutableListOf<Uri>()
    for (fileName in fileNames) {
      val destinationFile = File(soundDirectory, fileName)
      try {
        context.assets.open("default/media/$fileName").use { input ->
          destinationFile.outputStream().use { output -> input.copyTo(output) }
        }
        deployedUris.add(Uri.fromFile(destinationFile))
      } catch (e: Exception) {
        logger.e("Error while deploying the sound file [file=$fileName]", e)
      }
    }
    if (deployedUris.isNotEmpty()) {
      try {
        soundManager?.loadFiles(deployedUris)
      } catch (e: Exception) {
        logger.e("Error while loading the sound files", e)
      }
    }
  }

  private fun setLeds(leds: List<String>) {
    try {
      ledInterface?.set(leds)
    } catch (e: Exception) {
      logger.e("Error while setting the LEDs [leds=$leds]", e)
    }
  }

  private fun playSound(fileName: String) {
    try {
      soundManager?.startDefaultPlayingFile(fileName)
    } catch (e: Exception) {
      logger.e("Error while playing the sound [file=$fileName]", e)
    }
  }

  override fun signalSuccess() {
    setLeds(LEDS_SUCCESS)
    playSound(SOUND_SUCCESS)
  }

  override fun signalFailure() {
    setLeds(LEDS_FAILURE)
    playSound(SOUND_FAILURE)
  }

  override fun signalWaiting() {
    setLeds(LEDS_WAITING)
  }

  override fun release() {
    try {
      setLeds(LEDS_OFF)
    } catch (e: Exception) {
      logger.e("Error while releasing the feedback device", e)
    }
    joiner?.unbind()
    joiner = null
    ledInterface = null
    soundManager = null
  }
}
