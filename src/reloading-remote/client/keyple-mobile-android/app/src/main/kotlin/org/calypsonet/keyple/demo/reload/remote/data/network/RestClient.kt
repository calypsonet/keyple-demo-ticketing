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

import org.eclipse.keyple.distributed.MessageDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * REST API of the reloading server.
 *
 * Cannot directly extend SyncEndpointClient because retrofit allows API interfaces to extend
 * interfaces.
 */
interface RestClient {

  @GET("/card/sam-status") fun getSamStatus(): Call<SamStatus>

  @Headers("Accept: application/json", "Content-Type: application/json; charset=UTF-8")
  @POST("/card/remote-plugin")
  fun sendRequest(@Body msg: MessageDto?): Call<MutableList<MessageDto>>
}
