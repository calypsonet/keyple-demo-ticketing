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

import org.eclipse.keyple.core.util.HexUtil

/** AIDs of the Calypso applications selected by the demo (possibly truncated). */
object CalypsoAids {

  @JvmField val KEYPLE_GENERIC: ByteArray = HexUtil.toByteArray("A000000291FF9101")
  @JvmField val CD_LIGHT_GTML: ByteArray = HexUtil.toByteArray("315449432E49434131")
  @JvmField val CALYPSO_LIGHT: ByteArray = HexUtil.toByteArray("315449432E49434133")
  @JvmField val NORMALIZED_IDF: ByteArray = HexUtil.toByteArray("A0000004040125090101")

  /**
   * Implements the DF Name check method required by TL-SEL-AIDMATCH.1: the DF name must start with
   * the AID, followed by zeros only.
   */
  @JvmStatic
  fun matches(aid: ByteArray, dfName: ByteArray): Boolean =
      aid.size <= dfName.size &&
          aid.indices.all { aid[it] == dfName[it] } &&
          (aid.size until dfName.size).all { dfName[it] == 0.toByte() }
}
