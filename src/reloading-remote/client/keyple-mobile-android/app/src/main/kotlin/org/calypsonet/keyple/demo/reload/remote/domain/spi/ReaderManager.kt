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
