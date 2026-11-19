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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context
import com.famoco.keypleplugin.pcl.FamocoPclContactlessReader
import com.famoco.keypleplugin.pcl.FamocoPclPlugin
import com.famoco.keypleplugin.pcl.FamocoPclPluginFactoryProvider
import com.famoco.keypleplugin.pcl.FamocoPclReaderStateListener
import com.famoco.keypleplugin.pcl.enums.SupportedContactlessProtocols
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory

/**
 * Contactless reader of the Famoco FX9xx terminals (Ingenico Open1500 module connected by USB),
 * using the Famoco PCL plugin.
 */
internal object FamocoPclReaders {

  val PLUGIN_NAME: String = FamocoPclPlugin.PLUGIN_NAME
  val CARD_READER_NAME: String = FamocoPclContactlessReader.READER_NAME
  /**
   * Physical protocols of the Calypso cards (the plugin reports any active protocol as current).
   */
  val CALYPSO_CARD_PROTOCOLS: List<String> =
      listOf(
          SupportedContactlessProtocols.ISO_14443_B.name,
          SupportedContactlessProtocols.ISO_14443_A.name,
          SupportedContactlessProtocols.INNOVATRON.name)

  /**
   * Provides the plugin factory once the Open1500 module is connected: waits for it, the
   * registration being a blocking call made outside of the main thread.
   */
  fun provideFactory(context: Context, timeoutMs: Long): KeyplePluginExtensionFactory =
      runBlocking {
        val readerAvailable = CompletableDeferred<Unit>()
        val factory =
            FamocoPclPluginFactoryProvider()
                .getFactory(
                    context,
                    object : FamocoPclReaderStateListener {
                      override fun onReaderAvailable() {
                        readerAvailable.complete(Unit)
                      }

                      override fun onReaderUnavailable() {}
                    })
        withTimeout(timeoutMs) { readerAvailable.await() }
        factory
      }
}
