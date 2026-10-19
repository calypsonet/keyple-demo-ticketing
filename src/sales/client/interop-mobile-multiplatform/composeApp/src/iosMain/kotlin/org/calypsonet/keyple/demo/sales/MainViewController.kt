/* ******************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.sales

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.calypsonet.keyple.demo.sales.data.platform.Buzzer
import org.calypsonet.keyple.demo.sales.data.platform.DataStorePathProducer
import org.calypsonet.keyple.demo.sales.data.platform.PlatformBuzzer
import org.calypsonet.keyple.demo.sales.data.platform.createDataStore
import org.calypsonet.keyple.demo.sales.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.sales.data.service.ReadCardCache
import org.calypsonet.keyple.demo.sales.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.sales.data.service.ServerReadinessChecker
import org.eclipse.keyple.interop.localreader.nfcmobile.api.*
import platform.UIKit.UIDevice

val serverConfigRepository = ServerConfigRepository(createDataStore(DataStorePathProducer()))

val readCardCache = ReadCardCache()

val keypleInteropService =
    KeypleInteropService(
        reader =
            MultiplatformNfcReader(
                LocalNfcReader() { error ->
                  // Text displayed by the NFC sheet of iOS, the cause being logged
                  Napier.w("Card reading error [cause=${error.message}]")
                  return@LocalNfcReader "Card reading error"
                }),
        clientId = UIDevice.currentDevice.identifierForVendor?.UUIDString() ?: "anon",
        serverConfigRepository = serverConfigRepository)

val serverReadinessChecker = ServerReadinessChecker()

val buzzer = Buzzer(PlatformBuzzer())

val logger = Napier.base(DebugAntilog())

fun MainViewController() = ComposeUIViewController {
  App(keypleInteropService, readCardCache, serverConfigRepository, serverReadinessChecker, buzzer)
}
