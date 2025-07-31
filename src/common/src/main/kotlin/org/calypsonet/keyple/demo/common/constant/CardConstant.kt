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
package org.calypsonet.keyple.demo.common.constant

import org.eclipse.keyple.core.util.HexUtil

class CardConstant {
  companion object {

    // AIDs used for selection (could be truncated)
    val AID_KEYPLE_GENERIC = HexUtil.toByteArray("A000000291FF9101")
    val AID_CD_LIGHT_GTML = HexUtil.toByteArray("315449432E49434131")
    val AID_CALYPSO_LIGHT = HexUtil.toByteArray("315449432E49434133")
    val AID_NORMALIZED_IDF = HexUtil.toByteArray("A0000004040125090101")

    const val SFI_ENVIRONMENT_AND_HOLDER = 0x07.toByte()
    const val SFI_EVENTS_LOG = 0x08.toByte()
    const val SFI_CONTRACTS = 0x09.toByte()
    const val SFI_COUNTERS = 0x19.toByte()

    const val DEFAULT_KIF_PERSONALIZATION = 0x21.toByte()
    const val DEFAULT_KIF_LOAD = 0x27.toByte()
    const val DEFAULT_KIF_DEBIT = 0x30.toByte()

    const val ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES = 29
    const val CONTRACT_RECORD_SIZE_BYTES = 29
    const val EVENT_RECORD_SIZE_BYTES = 29

    val ALLOWED_FILE_STRUCTURES =
        listOf<Byte>(
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

    /** Implements the DF Name check method required by TL-SEL-AIDMATCH.1 */
    fun aidMatch(aid: ByteArray, dfName: ByteArray): Boolean {
      if (aid.size > dfName.size) {
        return false
      }
      var i = 0
      for (a in aid) {
        if (a != dfName[i++]) {
          return false
        }
      }
      for (j in i until dfName.size) {
        if (dfName[j] != 0.toByte()) {
          return false
        }
      }
      return true
    }

    const val SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK = 4
    const val SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK = 7
    const val SC_CONTRACT_FIRST_BLOCK = 8
    const val SC_COUNTER_LAST_BLOCK = 11
    const val SC_EVENT_FIRST_BLOCK = 12
    const val SC_EVENT_LAST_BLOCK = 15

    const val SC_ENVIRONMENT_AND_HOLDER_SIZE_BYTES = 16
    const val SC_CONTRACT_RECORD_SIZE_BYTES = 16
    const val SC_EVENT_RECORD_SIZE_BYTES = 16
  }
}
