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
package org.calypsonet.keyple.demo.sales.data.network

/**
 * Endpoint of the remote plugin of the server, the same as for the Android client: it is not
 * configurable.
 */
const val REMOTE_PLUGIN_ENDPOINT = "/card/remote-plugin"

data class KeypleServerConfig(
    val host: String,
    val port: Int,
    val basicAuth: String? = null,
) {
  fun baseUrl() = "${host}:${port}"

  fun serviceUrl() = "${baseUrl()}$REMOTE_PLUGIN_ENDPOINT"
}
