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

/** Layout of the storage cards with 4-byte blocks (MIFARE Ultralight and ST25 SRT512). */
object StorageCardBlocks {
  const val ENVIRONMENT_AND_HOLDER_FIRST_BLOCK = 4
  const val ENVIRONMENT_AND_HOLDER_LAST_BLOCK = 7
  const val CONTRACT_FIRST_BLOCK = 8
  const val COUNTER_LAST_BLOCK = 11
  const val EVENT_FIRST_BLOCK = 12
  const val EVENT_LAST_BLOCK = 15

  const val CONTRACT_RECORD_SIZE_BYTES = 16
  const val EVENT_RECORD_SIZE_BYTES = 16
}
