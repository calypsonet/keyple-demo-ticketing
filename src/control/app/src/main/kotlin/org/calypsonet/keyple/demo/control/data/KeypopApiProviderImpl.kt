package org.calypsonet.keyple.demo.control.data

import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.storagecard.StorageCardApiFactory

class KeypopApiProviderImpl : KeypopApiProvider {

    override fun getReaderApiFactory(): ReaderApiFactory {
        return SmartCardServiceProvider.getService().readerApiFactory
    }

    override fun getCalypsoCardApiFactory(): CalypsoCardApiFactory {
        return CalypsoExtensionService.getInstance().calypsoCardApiFactory
    }

    override fun getLegacySamApiFactory(): LegacySamApiFactory {
        return LegacySamExtensionService.getInstance().legacySamApiFactory
    }

    override fun getStorageCardApiFactory(): StorageCardApiFactory {
        return StorageCardExtensionService.getInstance().storageCardApiFactory
    }
}