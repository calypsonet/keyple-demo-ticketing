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
package org.calypsonet.keyple.demo.reload.remote.domain.spi

import org.calypsonet.keyple.demo.reload.remote.domain.model.ServerConfig

/**
 * Port checking that the reload server is ready to process the cards: reachable, and with its SAM
 * available.
 */
interface ServerReadinessChecker {

  /**
   * Checks the server at the provided address.
   *
   * @param serverConfig The address of the server.
   * @return True if the server is reachable and its SAM available, false if the SAM is not
   *   available.
   * @throws Exception If the server cannot be reached.
   */
  fun isServerReady(serverConfig: ServerConfig): Boolean
}
