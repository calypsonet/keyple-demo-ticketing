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
import org.eclipse.keyple.core.common.KeyplePluginExtensionFactory

/** Mock of the contactless reader of the Famoco FX9xx terminals (Famoco PCL plugin absent). */
internal object FamocoPclReaders {

  val PLUGIN_NAME: String = "FamocoPclPluginMock"
  val CARD_READER_NAME: String = "FamocoPclContactlessReaderMock"
  val CALYPSO_CARD_PROTOCOLS: List<String> = emptyList()

  fun provideFactory(context: Context, timeoutMs: Long): KeyplePluginExtensionFactory =
      throw UnsupportedOperationException("Famoco PCL plugin not available in this build")
}
