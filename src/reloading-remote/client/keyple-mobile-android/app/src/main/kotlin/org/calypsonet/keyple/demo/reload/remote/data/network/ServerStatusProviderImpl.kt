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
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Requests the SAM status of the server through its REST API.
 *
 * The client is built at each request, the server address being provided by the caller (e.g. an
 * address being entered in the settings).
 */
class ServerStatusProviderImpl : ServerStatusProvider {

  override fun isSamReady(serverConfig: ServerConfig): Boolean {
    val client =
        Retrofit.Builder()
            .baseUrl(serverConfig.url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RestClient::class.java)
    val response = client.getSamStatus().execute()
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    val samStatus = response.body() ?: throw IllegalStateException("Empty SAM status")
    return samStatus.isSamReady
  }
}
