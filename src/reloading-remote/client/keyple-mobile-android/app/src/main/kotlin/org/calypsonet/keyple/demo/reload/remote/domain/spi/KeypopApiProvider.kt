package org.calypsonet.keyple.demo.reload.remote.domain.spi

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