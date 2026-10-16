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
package org.calypsonet.keyple.demo.common.constants

/**
 * Layout of the MIFARE Classic 1K cards, in the sector 1 only (16-byte blocks): blocks 4, 5 and 6
 * are used, block 7 being the sector trailer (reserved).
 */
object MifareClassicBlocks {
  const val ENVIRONMENT_AND_HOLDER_BLOCK = 4
  const val CONTRACT_BLOCK = 5
  const val EVENT_BLOCK = 6

  /** Block used for the authentication (any block of the sector 1). */
  const val SECTOR_1_AUTH_BLOCK = 4

  /** Number of the key provided by the key provider for the authentication. */
  const val DEFAULT_KEY_NUMBER = 0
}
