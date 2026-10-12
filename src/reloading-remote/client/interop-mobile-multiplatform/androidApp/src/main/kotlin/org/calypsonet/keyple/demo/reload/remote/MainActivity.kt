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
package org.calypsonet.keyple.demo.reload.remote

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.calypsonet.keyple.demo.reload.remote.platform.Buzzer
import org.calypsonet.keyple.demo.reload.remote.platform.DataStorePathProducer
import org.calypsonet.keyple.demo.reload.remote.platform.PlatformBuzzer
import org.calypsonet.keyple.demo.reload.remote.platform.createDataStore
import org.calypsonet.keyple.demo.reload.remote.service.CardRepository
import org.calypsonet.keyple.demo.reload.remote.service.KeypleService
import org.calypsonet.keyple.demo.reload.remote.service.ServerConfigRepository
import org.eclipse.keyple.interop.localreader.nfcmobile.api.LocalNfcReader
import org.eclipse.keyple.interop.localreader.nfcmobile.api.MultiplatformNfcReader

class MainActivity : ComponentActivity() {
  @SuppressLint("HardwareIds")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val cardRepository = CardRepository()
    val serverConfigRepository =
        ServerConfigRepository(createDataStore(DataStorePathProducer(applicationContext)))
    val keypleService =
        KeypleService(
            reader = MultiplatformNfcReader(LocalNfcReader(this)),
            clientId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
            serverConfigRepository = serverConfigRepository,
            cardRepository = cardRepository,
            buzzer = Buzzer(PlatformBuzzer(applicationContext)))

    setContent { App(keypleService, cardRepository, serverConfigRepository) }
  }
}
