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
package org.calypsonet.keyple.demo.common.model.type

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Data types of the data model (DateCompact, TimeCompact, PriorityCode, VersionNumber). */
class DataTypesTest {

  @Test
  fun dateCompact_isTheNumberOfDaysSinceJanuary1st2010() {
    assertThat(DateCompact(LocalDate.of(2010, 1, 1)).value).isZero()
    assertThat(DateCompact(LocalDate.of(2010, 1, 2)).value).isEqualTo(1)
    assertThat(DateCompact(16383).date).isEqualTo(LocalDate.of(2054, 11, 9))
  }

  @Test
  fun dateCompact_isConvertedBackToTheSameDate() {
    val date = LocalDate.of(2026, 10, 15)

    assertThat(DateCompact(date).date).isEqualTo(date)
  }

  @Test
  fun timeCompact_isTheNumberOfMinutesOfTheDay_withoutTheSeconds() {
    assertThat(TimeCompact(LocalDateTime.of(2026, 10, 15, 0, 0)).value).isZero()
    assertThat(TimeCompact(LocalDateTime.of(2026, 10, 15, 14, 30, 59)).value).isEqualTo(870)
    assertThat(TimeCompact(LocalDateTime.of(2026, 10, 15, 23, 59)).value).isEqualTo(1439)
  }

  @Test
  fun priorityCode_isFoundByItsCode() {
    assertThat(PriorityCode.fromCode(0)).isEqualTo(PriorityCode.FORBIDDEN)
    assertThat(PriorityCode.fromCode(1)).isEqualTo(PriorityCode.SEASON_PASS)
    assertThat(PriorityCode.fromCode(2)).isEqualTo(PriorityCode.MULTI_TRIP)
    assertThat(PriorityCode.fromCode(31)).isEqualTo(PriorityCode.EXPIRED)
  }

  @Test
  fun priorityCode_notSupportedByTheDemo_isUnknown() {
    // 3: stored value, defined by the data model but not supported by the demo; 4 to 30: RFU
    assertThat(PriorityCode.fromCode(3)).isEqualTo(PriorityCode.UNKNOWN)
    assertThat(PriorityCode.fromCode(30)).isEqualTo(PriorityCode.UNKNOWN)
  }

  @Test
  fun versionNumber_isFoundByItsCode() {
    assertThat(VersionNumber.fromCode(0)).isEqualTo(VersionNumber.UNDEFINED)
    assertThat(VersionNumber.fromCode(1)).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(VersionNumber.fromCode(255)).isEqualTo(VersionNumber.RESERVED)
    assertThat(VersionNumber.fromCode(2)).isEqualTo(VersionNumber.UNKNOWN)
  }
}
