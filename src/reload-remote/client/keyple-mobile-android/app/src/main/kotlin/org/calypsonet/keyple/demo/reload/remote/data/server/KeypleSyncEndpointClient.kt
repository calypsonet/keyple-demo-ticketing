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
package org.calypsonet.keyple.demo.reload.remote.data.server

import okhttp3.OkHttpClient
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.eclipse.keyple.distributed.MessageDto
import org.eclipse.keyple.distributed.spi.SyncEndpointClientSpi
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * We have to wrap the retrofit client.
 *
 * The request is executed synchronously, as expected by the Keyple synchronous endpoint (the remote
 * service is executed by the application outside the main thread).
 *
 * The request is sent to the server currently configured in the settings: a change of the server
 * address is taken into account without restarting the application.
 */
class KeypleSyncEndpointClient(
    private val appSettings: AppSettingsRepository,
    private val httpClient: OkHttpClient,
    private val logger: Logger
) : SyncEndpointClientSpi {

  private var serverRestApi: ServerRestApi? = null
  private var serverRestApiUrl: String? = null

  override fun sendRequest(message: MessageDto?): MutableList<MessageDto> {
    val response = getServerRestApi().sendRequest(message).execute()
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    return response.body() ?: throw IllegalStateException("Empty response from the server")
  }

  /** Returns the REST client of the configured server, created again when its address changes. */
  @Synchronized
  private fun getServerRestApi(): ServerRestApi {
    val serverUrl = appSettings.serverConfig.url
    val currentServerRestApi = serverRestApi
    if (currentServerRestApi != null && serverUrl == serverRestApiUrl) {
      return currentServerRestApi
    }
    logger.i("REST client created [url=$serverUrl]")
    val newServerRestApi =
        Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerRestApi::class.java)
    serverRestApi = newServerRestApi
    serverRestApiUrl = serverUrl
    return newServerRestApi
  }
}
