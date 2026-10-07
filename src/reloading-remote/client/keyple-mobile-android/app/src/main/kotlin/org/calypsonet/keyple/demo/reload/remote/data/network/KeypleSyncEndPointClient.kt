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
package org.calypsonet.keyple.demo.reload.remote.data.network

import org.eclipse.keyple.distributed.MessageDto
import org.eclipse.keyple.distributed.spi.SyncEndpointClientSpi
import retrofit2.HttpException

/**
 * We have to wrap the retrofit client.
 *
 * The request is executed synchronously, as expected by the Keyple synchronous endpoint (the remote
 * service is executed by the application outside the main thread).
 */
class KeypleSyncEndPointClient(private val restClient: RestClient) : SyncEndpointClientSpi {

  override fun sendRequest(msg: MessageDto?): MutableList<MessageDto> {
    val response = restClient.sendRequest(msg).execute()
    if (!response.isSuccessful) {
      throw HttpException(response)
    }
    return response.body() ?: throw IllegalStateException("Empty response from the server")
  }
}
