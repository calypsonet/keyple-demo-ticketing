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
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

/**
 * Readers of the terminal: registration of the reader plugins of the platform, then access to the
 * card reader and to the SAM reader.
 *
 * The readers are driven (card detection, selection, and processing) by the `CardReaderSession` of
 * the domain.
 */
interface TerminalReaders {
  /**
   * Registers the appropriate reader plugin(s) for the given reader type and activity. Must be
   * called before initializing readers. Blocking call (creation and registration of the plugins),
   * to be made outside of the main thread.
   *
   * @param terminalType The type of reader to use (for example, contactless reader).
   * @param uiContext UI context used to access platform-specific facilities.
   */
  fun registerPlugin(terminalType: TerminalType, uiContext: UiContext)

  fun initCardReader(): CardReader?

  fun getCardReader(): CardReader?

  fun initSamReaders(): List<CardReader>

  fun getSamReader(): CardReader?

  fun isStorageCardSupported(): Boolean

  /**
   * Releases resources and unregisters observers.
   *
   * @param observer Optional observer previously registered on the reader.
   */
  fun release(observer: CardReaderObserverSpi?)
}
