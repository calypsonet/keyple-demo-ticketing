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
package org.calypsonet.keyple.demo.control.domain.spi

import org.calypsonet.keyple.demo.control.domain.model.ReaderType

/** Port providing the user feedback of the terminal (sounds, LEDs...). */
interface UiManager {

  /**
   * Initializes the feedback resources for the given reader type.
   *
   * @param readerType The type of reader (terminal) the application runs on.
   * @param uiContext Platform-specific context used to access the feedback facilities.
   */
  fun init(readerType: ReaderType, uiContext: UiContext)

  /** Displays feedback for a successful result. */
  fun displayResultSuccess()

  /** Displays feedback for a failed result. */
  fun displayResultFailed()

  /** Releases the feedback resources. */
  fun release()
}
