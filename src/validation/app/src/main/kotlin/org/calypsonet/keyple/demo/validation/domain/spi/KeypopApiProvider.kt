/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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

import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.storagecard.StorageCardApiFactory

/**
 * Provider of Keypop API factories used by the domain.
 *
 * This abstraction decouples the domain from the concrete way these factories are obtained on a
 * given platform (e.g., via service loaders, dependency injection, or static calls).
 */
interface KeypopApiProvider {

  /** Returns the factory to create reader-related components (readers, selectors, managers...). */
  fun getReaderApiFactory(): ReaderApiFactory

  /** Returns the factory for Calypso card APIs. */
  fun getCalypsoCardApiFactory(): CalypsoCardApiFactory

  /** Returns the factory for legacy SAM APIs used to secure Calypso transactions. */
  fun getLegacySamApiFactory(): LegacySamApiFactory

  /** Returns the factory for storage card APIs (e.g., MIFARE Ultralight, ST25...). */
  fun getStorageCardApiFactory(): StorageCardApiFactory
}
