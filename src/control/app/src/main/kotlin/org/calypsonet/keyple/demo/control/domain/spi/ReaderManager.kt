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
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

interface ReaderManager {
  /**
   * Registers the appropriate reader plugin(s) for the given reader type and activity. Must be
   * called before initializing readers.
   *
   * @param readerType The type of reader to use (e.g. contactless reader).
   * @param uiContext UI context used to access platform-specific facilities.
   */
  fun registerPlugin(readerType: ReaderType, uiContext: UiContext)

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
  fun onDestroy(observer: CardReaderObserverSpi?)

  fun clear()

  fun displayResultSuccess(): Boolean

  fun displayResultFailed(): Boolean
}
