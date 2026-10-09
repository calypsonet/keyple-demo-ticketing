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

/** Files of the Calypso applications used by the demo. */
object CalypsoFiles {

  const val SFI_ENVIRONMENT_AND_HOLDER = 0x07.toByte()
  const val SFI_EVENTS_LOG = 0x08.toByte()
  const val SFI_CONTRACTS = 0x09.toByte()
  const val SFI_COUNTERS = 0x19.toByte()

  const val ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES = 29
  const val CONTRACT_RECORD_SIZE_BYTES = 29
  const val EVENT_RECORD_SIZE_BYTES = 29

  /** File structures (application subtypes) supported by the demo. */
  @JvmField
  val ALLOWED_FILE_STRUCTURES: Set<Byte> =
      setOf(
          0x01, // Revision 2 minimum
          0x02, // Revision 2 minimum with MF files
          0x03, // Revision 2 extended
          0x04, // Revision 2 extended with MF files
          0x05, // CD Light/GTML Compatibility
          0x06, // CD97 Structure 2 Compatibility
          0x07, // CD97 Structure 3 Compatibility
          0x08, // Extended Ticketing with Loyalty
          0x09, // Extended Ticketing with Loyalty and Miscellaneous
          0x32, // Calypso Light Classic file structure
          0x33) // Calypso Basic file structure
}
