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
package org.calypsonet.keyple.demo.validation.domain.spi

import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi

interface ReaderManager {

  fun registerPlugin(readerType: ReaderType, uiContext: UiContext)

  fun initCardReader(): CardReader?

  fun getCardReader(): CardReader?

  fun initSamReaders(): List<CardReader>

  fun getSamReader(): CardReader?

  fun isStorageCardSupported(): Boolean

  fun onDestroy(observer: CardReaderObserverSpi?)

  fun displayResultSuccess(): Boolean

  fun displayResultFailed(): Boolean
}
