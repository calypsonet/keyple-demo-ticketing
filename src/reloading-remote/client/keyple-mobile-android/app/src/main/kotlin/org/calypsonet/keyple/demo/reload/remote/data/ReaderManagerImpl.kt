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
package org.calypsonet.keyple.demo.reload.remote.data

import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import kotlin.jvm.Throws
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ObservableCardReader
import org.eclipse.keypop.reader.ReaderCommunicationException
import timber.log.Timber

/**
 * Manager provided to encapsulate slight differences between readers provides methods to improve
 * code readability.
 */
object ReaderManagerImpl : ReaderManager {

  /** Register any keyple plugin */
  override fun registerPlugin(factory: KeyplePluginExtensionFactory): Plugin? {
    return try {
      SmartCardServiceProvider.getService().registerPlugin(factory)
    } catch (_: Exception) {
      null
    }
  }

  /** Unregister any keyple plugin */
  override fun unregisterPlugin(pluginName: String) {
    try {
      SmartCardServiceProvider.getService().unregisterPlugin(pluginName)
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

  /** Retrieve a registered reader */
  @Throws(ReaderCommunicationException::class)
  override fun getReader(readerName: String): CardReader {
    var reader: CardReader? = null
    SmartCardServiceProvider.getService().plugins.forEach { reader = it.getReader(readerName) }
    return reader ?: throw ReaderCommunicationException("$readerName not found")
  }

  /** Retrieve a registered observable reader. */
  @Throws(Exception::class)
  override fun getObservableReader(readerName: String): ObservableCardReader {
    val reader = getReader(readerName)
    return reader as? ObservableCardReader ?: throw Exception("$readerName not found")
  }
}
