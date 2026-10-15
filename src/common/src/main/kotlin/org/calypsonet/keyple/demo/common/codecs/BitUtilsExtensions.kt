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

/**
 * Writes the provided integer value, unsigned, on the provided number of bits (multiple of 8), as
 * its last bytes in big-endian order (`setNextInteger` of the library being limited to 31 bits).
 *
 * The bytes written are exactly those of the field: a sign byte added before a value whose high bit
 * is set (e.g. by `BigInteger.toByteArray`) would shift the value and truncate it.
 */
internal fun BitUtils.setNextValue(value: Int, size: Int) {
  require(size % Byte.SIZE_BITS == 0) { "Size not a multiple of 8 bits [size=$size]" }
  val unsignedValue = value.toLong() and 0xFFFFFFFFL
  val byteCount = size / Byte.SIZE_BITS
  val bytes =
      ByteArray(byteCount) {
        val shift = Byte.SIZE_BITS * (byteCount - 1 - it)
        if (shift < Long.SIZE_BITS) (unsignedValue ushr shift).toByte() else 0
      }
  setNextByte(bytes, size)
}
