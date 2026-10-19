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
package org.calypsonet.keyple.demo.sales.data.server

import org.calypsonet.keyple.demo.sales.domain.model.ServerConfig
import org.calypsonet.keyple.demo.sales.domain.spi.ServerReadinessChecker
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Requests the availability of the SAM of the server through its REST API
 * (`/card/sam-availability`).
 *
 * The client is built at each request, the server address being provided by the caller (for
 * example, an address being entered in the settings).
 */
class RestServerReadinessChecker : ServerReadinessChecker {

  override fun isServerReady(serverConfig: ServerConfig): Boolean {
    val client =
        Retrofit.Builder()
            .baseUrl(serverConfig.url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ServerRestApi::class.java)
    val response = client.getSamAvailability().execute()
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    val samAvailability = response.body() ?: throw IllegalStateException("Empty SAM availability")
    return samAvailability.isSamAvailable
  }
}
