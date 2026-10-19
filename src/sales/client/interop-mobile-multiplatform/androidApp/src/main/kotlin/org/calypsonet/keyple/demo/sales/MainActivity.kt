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

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.calypsonet.keyple.demo.sales.data.platform.Buzzer
import org.calypsonet.keyple.demo.sales.data.platform.DataStorePathProducer
import org.calypsonet.keyple.demo.sales.data.platform.PlatformBuzzer
import org.calypsonet.keyple.demo.sales.data.platform.createDataStore
import org.calypsonet.keyple.demo.sales.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.sales.data.service.ReadCardCache
import org.calypsonet.keyple.demo.sales.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.sales.data.service.ServerReadinessChecker
import org.eclipse.keyple.interop.localreader.nfcmobile.api.LocalNfcReader
import org.eclipse.keyple.interop.localreader.nfcmobile.api.MultiplatformNfcReader

class MainActivity : ComponentActivity() {
  @SuppressLint("HardwareIds")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val readCardCache = ReadCardCache()
    val serverConfigRepository =
        ServerConfigRepository(createDataStore(DataStorePathProducer(applicationContext)))
    val keypleInteropService =
        KeypleInteropService(
            reader = MultiplatformNfcReader(LocalNfcReader(this)),
            clientId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
            serverConfigRepository = serverConfigRepository)
    val serverReadinessChecker = ServerReadinessChecker()
    val buzzer = Buzzer(PlatformBuzzer(applicationContext))

    setContent {
      App(
          keypleInteropService,
          readCardCache,
          serverConfigRepository,
          serverReadinessChecker,
          buzzer)
    }
  }
}
