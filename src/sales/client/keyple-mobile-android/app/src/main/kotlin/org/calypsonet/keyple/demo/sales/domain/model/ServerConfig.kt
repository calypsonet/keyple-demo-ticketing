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
package org.calypsonet.keyple.demo.sales.domain.model

/** Address of the sales server. */
data class ServerConfig(val protocol: String, val ip: String, val port: Int) {

  /** Base URL of the server (for example, "http://192.168.0.1:8080"). */
  val url: String
    get() = "$protocol$ip:$port"
}
