package org.calypsonet.keyple.demo.reload.remote.domain.spi

import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader

interface ReaderManager {
    fun registerPlugin(factory: KeyplePluginExtensionFactory): Plugin?

    fun unregisterPlugin(pluginName: String)

    fun getReader(readerName: String): CardReader

    fun getObservableReader(readerName: String): ObservableCardReader
}