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
package org.calypsonet.keyple.demo.inspection.domain.spi

import org.calypsonet.keyple.demo.inspection.domain.model.TerminalType

/** Port providing the user feedback of the terminal (sounds, LEDs, etc.). */
interface UserFeedback {

  /**
   * Initializes the feedback resources for the given reader type.
   *
   * @param terminalType The type of reader (terminal) the application runs on.
   * @param uiContext Platform-specific context used to access the feedback facilities.
   */
  fun init(terminalType: TerminalType, uiContext: UiContext)

  /** Signals a successful result to the user (sound, LEDs, etc.). */
  fun signalSuccess()

  /** Signals a failed result to the user (sound, LEDs, etc.). */
  fun signalFailure()

  /** Releases the feedback resources. */
  fun release()
}
