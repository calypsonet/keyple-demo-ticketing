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
package org.calypsonet.keyple.demo.validation.data

import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.storagecard.StorageCardApiFactory

class KeypopApiProvider {

  fun getReaderApiFactory(): ReaderApiFactory {
    return SmartCardServiceProvider.getService().readerApiFactory
  }

  fun getCalypsoCardApiFactory(): CalypsoCardApiFactory {
    return CalypsoExtensionService.getInstance().calypsoCardApiFactory
  }

  fun getLegacySamApiFactory(): LegacySamApiFactory {
    return LegacySamExtensionService.getInstance().legacySamApiFactory
  }

  fun getStorageCardApiFactory(): StorageCardApiFactory {
    return StorageCardExtensionService.getInstance().storageCardApiFactory
  }
}
