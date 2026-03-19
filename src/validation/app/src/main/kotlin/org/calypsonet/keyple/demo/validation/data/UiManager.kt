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
 * Manages the User Interface (UI) feedback for the validation application, handling visual (LEDs)
 * and auditory (sounds) indications. This interface provides a common API for different terminal
 * implementations.
 */
interface UiManager {
  /**
   * Initializes the UI manager.
   *
   * @param onReady Callback to be invoked when initialization is complete.
   */
  fun init(onReady: () -> Unit = {})

  /** Displays feedback for a successful result. */
  fun displayResultSuccess()

  /** Displays feedback for a failed result. */
  fun displayResultFailed()

  /** Displays feedback indicating the terminal is waiting for a card. */
  fun displayWaiting()

  /** Releases resources held by the UI manager. */
  fun release()
}
