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
package org.calypsonet.keyple.demo.sales.domain.spi

import org.calypsonet.keyple.demo.sales.domain.model.TerminalType
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/**
 * Readers of the terminal: registration of the reader plugins of the platform, then access to the
 * contactless card reader.
 *
 * The readers are driven (card detection, selection, and processing) by the `CardReaderSession` of
 * the domain.
 */
interface TerminalReaders {
  /**
   * Returns the name of the card reader of the given terminal type.
   *
   * @param terminalType The type of terminal.
   */
  fun getReaderName(terminalType: TerminalType): String

  fun registerPlugin(terminalType: TerminalType, uiContext: UiContext)

  fun initCardReader(
      observer: CardReaderObserverSpi?,
      readerObservationExceptionHandler: CardReaderObservationExceptionHandlerSpi?
  ): CardReader?

  fun getReader(readerName: String): CardReader

  fun getCardReader(): CardReader?

  fun release(observer: CardReaderObserverSpi?)
}
