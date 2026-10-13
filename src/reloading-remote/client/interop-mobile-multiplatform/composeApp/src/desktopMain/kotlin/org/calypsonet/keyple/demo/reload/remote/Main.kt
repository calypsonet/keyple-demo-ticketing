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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.aakira.napier.Napier
import org.calypsonet.keyple.demo.reload.remote.data.platform.Buzzer
import org.calypsonet.keyple.demo.reload.remote.data.platform.DataStorePathProducer
import org.calypsonet.keyple.demo.reload.remote.data.platform.PlatformBuzzer
import org.calypsonet.keyple.demo.reload.remote.data.platform.createDataStore
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.data.service.ReadCardCache
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerConfigRepository
import org.calypsonet.keyple.demo.reload.remote.data.service.ServerReadinessChecker
import org.eclipse.keyple.interop.localreader.nfcmobile.api.LocalNfcReader
import org.eclipse.keyple.interop.localreader.nfcmobile.api.MultiplatformNfcReader

fun main(args: Array<String>) = application {
  initLogger()
  var filter = "*"

  val windowState =
      rememberWindowState(placement = WindowPlacement.Floating, width = 500.dp, height = 850.dp)
  for (arg: String in args) {
    if (arg.startsWith("-filter=")) {
      filter = arg.removePrefix("-filter=")
      Napier.d("Filter reader with: $filter")
    }
  }

  val readCardCache = ReadCardCache()
  val serverConfigRepository = ServerConfigRepository(createDataStore(DataStorePathProducer()))
  val keypleInteropService =
      KeypleInteropService(
          reader = MultiplatformNfcReader(LocalNfcReader(filter)),
          clientId = "SOMEID",
          serverConfigRepository = serverConfigRepository)
  val serverReadinessChecker = ServerReadinessChecker()
  val buzzer = Buzzer(PlatformBuzzer())

  Window(
      // icon = TODO
      title = "Keyple Demo Reload Remote",
      state = windowState,
      onCloseRequest = ::exitApplication) {
        App(
            keypleInteropService,
            readCardCache,
            serverConfigRepository,
            serverReadinessChecker,
            buzzer)
      }
}
