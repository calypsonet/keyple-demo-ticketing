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
package org.calypsonet.keyple.demo.reload.remote.data.network

import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider
import org.eclipse.keyple.core.util.json.JsonUtil
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/** Requests the SAM status of the server through its REST API. */
class ServerStatusProviderImpl : ServerStatusProvider {

  override fun isSamReady(serverConfig: ServerConfig): Boolean {
    val client =
        Retrofit.Builder()
            .baseUrl(serverConfig.url)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(RestClient::class.java)
    val response = client.ping().blockingGet()
    return JsonUtil.getParser().fromJson(response, SamStatus::class.java).isSamReady
  }
}
