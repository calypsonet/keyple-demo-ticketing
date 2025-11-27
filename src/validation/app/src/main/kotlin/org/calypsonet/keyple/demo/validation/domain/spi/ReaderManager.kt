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

/**
 * Manages lifecycle and access to card and SAM readers for the domain.
 *
 * Implementations encapsulate reader plugin registration, reader initialization, detection control
 * and basic UI feedback hooks, abstracting the underlying platform (e.g. Android, desktop).
 */
interface ReaderManager {

  /**
   * Registers the appropriate reader plugin(s) for the given reader type and UI context. Must be
   * called before initializing readers.
   *
   * @param readerType The type of reader to use (e.g. contactless reader).
   * @param uiContext UI context used to access platform-specific facilities.
   */
  fun registerPlugin(readerType: ReaderType, uiContext: UiContext)

  /**
   * Initializes and returns the primary card reader (contactless). Returns null if not available.
   */
  fun initCardReader(): CardReader?

  /** Returns the previously initialized primary card reader or null if not available. */
  fun getCardReader(): CardReader?

  /**
   * Initializes and returns the list of available SAM readers. The first reader is typically used
   * for SAM access.
   */
  fun initSamReaders(): List<CardReader>

  /** Returns the selected SAM reader, if any. */
  fun getSamReader(): CardReader?

  /** Indicates whether storage cards (e.g., MIFARE Ultralight, ST25 SRT512) are supported. */
  fun isStorageCardSupported(): Boolean

  /**
   * Releases resources and unregisters observers.
   *
   * @param observer Optional observer previously registered on the reader.
   */
  fun onDestroy(observer: CardReaderObserverSpi?)

  /**
   * Triggers a success feedback in the UI (sound, vibration, message...).
   *
   * @return true if the feedback was handled by the UI layer, false otherwise.
   */
  fun displayResultSuccess(): Boolean

  /**
   * Triggers a failure feedback in the UI (sound, vibration, message...).
   *
   * @return true if the feedback was handled by the UI layer, false otherwise.
   */
  fun displayResultFailed(): Boolean
}
