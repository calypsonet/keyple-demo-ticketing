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

import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi

/**
 * Handler of the unexpected errors of the observation of the card reader.
 *
 * After such an error, Keyple ends the monitoring job of the reader without leaving its current
 * state: the reader no longer detects the cards until its detection is stopped and restarted (for
 * example, read timeout of the Open1500 module turned into an `IllegalArgumentException` by the
 * Famoco PCL plugin). The error is logged, then notified to the [listener], in charge of restarting
 * the detection.
 */
class ReaderObservationErrorHandler(private val logger: Logger) :
    CardReaderObservationExceptionHandlerSpi {

  /** Listener of the errors, called from the monitoring thread of the reader. */
  @Volatile var listener: (() -> Unit)? = null

  override fun onReaderObservationError(pluginName: String?, readerName: String?, e: Throwable?) {
    logger.e("Unexpected reader error [plugin=$pluginName, reader=$readerName]", e)
    listener?.invoke()
  }
}
