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

/**
 * Feedback device of the terminal, giving the user feedback of the validation with LEDs and sounds.
 * This interface provides a common API for different terminal implementations.
 */
internal interface FeedbackDevice {
  /**
   * Initializes the feedback device.
   *
   * @param onReady Callback to be invoked when initialization is complete.
   */
  fun init(onReady: () -> Unit = {})

  /** Signals a successful result to the user (sound, LEDs...). */
  fun signalSuccess()

  /** Signals a failed result to the user (sound, LEDs...). */
  fun signalFailure()

  /** Signals that the terminal is waiting for a card. */
  fun signalWaiting()

  /** Releases resources held by the feedback device. */
  fun release()
}
