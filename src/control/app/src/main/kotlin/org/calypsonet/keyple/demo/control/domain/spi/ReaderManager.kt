package org.calypsonet.keyple.demo.control.domain.spi

import org.calypsonet.keyple.demo.control.domain.model.ReaderType
import org.eclipse.keypop.reader.CardReader

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

    fun clear()

    fun displayResultSuccess(): Boolean

    fun displayResultFailed(): Boolean
}