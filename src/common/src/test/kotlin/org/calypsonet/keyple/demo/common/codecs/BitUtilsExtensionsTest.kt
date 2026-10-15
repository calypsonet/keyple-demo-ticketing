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
package org.calypsonet.keyple.demo.common.codecs

import fr.devnied.bitlib.BitUtils
import fr.devnied.bitlib.BytesUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Writing of the fields of the records, as unsigned integers. */
class BitUtilsExtensionsTest {

  private fun encode(value: Int, size: Int): String {
    val bitUtils = BitUtils(size)
    bitUtils.setNextValue(value, size)
    return BytesUtils.bytesToString(bitUtils.data)
  }

  @Test
  fun valueWithTheHighBitSet_isWrittenWithoutSignByte() {
    assertThat(encode(0xFFFFFF, 24)).isEqualTo("FF FF FF")
    assertThat(encode(0x800000, 24)).isEqualTo("80 00 00")
    assertThat(encode(255, 8)).isEqualTo("FF")
  }

  @Test
  fun smallValue_isWrittenOnTheLastBytesOfTheField() {
    assertThat(encode(5, 24)).isEqualTo("00 00 05")
    assertThat(encode(1439, 16)).isEqualTo("05 9F")
  }

  @Test
  fun valueOn32Bits_isWrittenUnsigned() {
    assertThat(encode(Int.MAX_VALUE, 32)).isEqualTo("7F FF FF FF")
    assertThat(encode(-1, 32)).isEqualTo("FF FF FF FF")
  }

  @Test
  fun padding_isWrittenWithZeros() {
    assertThat(encode(0, 120)).isEqualTo("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00")
  }
}
