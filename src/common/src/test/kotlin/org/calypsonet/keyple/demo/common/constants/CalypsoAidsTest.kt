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

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.keyple.core.util.HexUtil
import org.junit.jupiter.api.Test

/** DF name check of the selected application (TL-SEL-AIDMATCH.1). */
class CalypsoAidsTest {

  private val aid = CalypsoAids.CD_LIGHT_GTML

  @Test
  fun dfNameEqualToTheAid_matches() {
    assertThat(CalypsoAids.matches(aid, HexUtil.toByteArray("315449432E49434131"))).isTrue()
  }

  @Test
  fun dfNameStartingWithTheAid_followedByZeros_matches() {
    assertThat(CalypsoAids.matches(aid, HexUtil.toByteArray("315449432E494341310000"))).isTrue()
  }

  @Test
  fun dfNameStartingWithTheAid_followedByOtherBytes_doesNotMatch() {
    assertThat(CalypsoAids.matches(aid, HexUtil.toByteArray("315449432E494341310001"))).isFalse()
  }

  @Test
  fun dfNameShorterThanTheAid_orDifferent_doesNotMatch() {
    assertThat(CalypsoAids.matches(aid, HexUtil.toByteArray("315449432E4943"))).isFalse()
    assertThat(CalypsoAids.matches(aid, CalypsoAids.CALYPSO_LIGHT)).isFalse()
  }
}
