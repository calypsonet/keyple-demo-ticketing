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
package org.calypsonet.keyple.demo.common.constants

import org.eclipse.keyple.core.util.HexUtil

class CardConstants {
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

    val PCA_PUBLIC_KEY_REFERENCE: ByteArray =
        HexUtil.toByteArray("0BA000000291A0000101B0010000000000000000000000000000000002")

    val PCA_PUBLIC_KEY: ByteArray =
        HexUtil.toByteArray(
            ("C2494557ECE5979A497424833489CCCACF4DEE3FD7576A99C3999D8F468174E7" +
                "6F393D4E5C3802AC6C3CB192EB687F5505D24EBA01FFC60D5752CE6910D50B4A" +
                "DAC8C93159165109C3901FCA383A9F6603D576390FD59899A10873936D3A369B" +
                "3EB8403ADFF476547B039ACC7DCB3C1FAF4F954E29A8C2E2AED7721272AF5CDC" +
                "0A3B2994715261A4364EC1256D00004E084914DC4727349D715C3848D7C54AD5" +
                "8DB0F6907549FED51D564E3A853D44F071A852AB536356C7974B16FC03E1FFE9" +
                "DEE7527FBADDA5BC1116156DBFA5C13F06ACBBCDCEE3F9F4564034A8AD20F407" +
                "32B2AB414891D940ED96DA6DA6E98F766A1CDBC7FD0C17A708BD5F68B816AA47"))

    val CA_CERTIFICATE: ByteArray =
        HexUtil.toByteArray(
            ("90010BA000000291A0000101B00100000000000000000000000000000000020B" +
                "A000000291A00100024001000000000000000000AEC8E0EA0000000020240222" +
                "00000000090100000000FF00000000000000000000000000000000000000AE0E" +
                "22FC13DA303EDEC0B02E89FC5BCDD1CED8123BAD3877C2C68BDB162C5C63DF6F" +
                "A9BE454ADD615D42D1FD4372A87F0368F0F2603C6CB12CFE3583891D2DA71185" +
                "FC9E3EB9894BD60447CA88200ED35E42AB08EC8606E0782D6005AEE9D282EE1B" +
                "98510E39D747C5070E383E8519720CD79F123B584E3DB31E05A6348369347EF0" +
                "D8C4E38A4553C26B518F235E4459534A990C680F596A19DF87C08F8124B8EA64" +
                "E1245A38BA31A2D400B36CEC7E72C5EE4EDD4C3FA7D2C8BB2A631609C341EF91" +
                "87FF80D21CF417EBE9328D07CA64F4AA40250B285559041BC64D24F5CCCC90B0" +
                "6C8EFFF0C80BADAB4D2D2ABBD21241490805A27AF1B41A282D67D61885CBDD23" +
                "F87271ABD1989C954B3146AE38AE2581DEFE8D48840F9075B9430CDD8ECB1916"))
  }
}
