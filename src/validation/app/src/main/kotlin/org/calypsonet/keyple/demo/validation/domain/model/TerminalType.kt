/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.domain.model

enum class TerminalType {
  ARRIVE,
  BLUEBIRD,
  /** Famoco terminal whose SAM is accessed with the Famoco SE Communication plugin. */
  FAMOCO,
  /** Famoco terminal whose SAM is accessed with the Android OMAPI plugin (e.g. FX207). */
  FAMOCO_OMAPI
}
