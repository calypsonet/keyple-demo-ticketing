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
package org.calypsonet.keyple.demo.common.parser

import fr.devnied.bitlib.BytesUtils
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.junit.jupiter.api.Test

class EnvironmentHolderStructureParserTest {

  private val envParser = EnvironmentHolderStructureParser()

  @Test
  fun parseEnv1() {
    val content = BytesUtils.fromString(DATA_ENV_1)

    val environment = envParser.parse(content)

    assertThat(environment).isNotNull
    assertThat(environment.envVersionNumber).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(environment.envApplicationNumber).isEqualTo(1)
    assertThat(environment.envIssuingDate.value).isEqualTo(4091)
    assertThat(environment.envEndDate.value).isEqualTo(7314)
    assertThat(environment.envIssuingDate.getDate()).isEqualTo(LocalDate.of(2021, 3, 15))
    assertThat(environment.envEndDate.getDate()).isEqualTo(LocalDate.of(2030, 1, 10))
    assertThat(environment.holderCompany).isEqualTo(7)
    assertThat(environment.holderIdNumber).isEqualTo(8)
  }

  @Test
  fun generateEnv1() {
    val envIssuingDate = LocalDate.of(2021, 3, 15)
    val envEndDate = LocalDate.of(2030, 1, 10)

    val environment =
        EnvironmentHolderStructure(
            envVersionNumber = VersionNumber.CURRENT_VERSION,
            envApplicationNumber = 1,
            envIssuingDate = DateCompact(envIssuingDate),
            envEndDate = DateCompact(envEndDate),
            holderCompany = 7,
            holderIdNumber = 8)

    val content = EnvironmentHolderStructureParser().generate(environment)

    assertThat(BytesUtils.bytesToString(content)).isEqualTo(DATA_ENV_1)
  }

  @Test
  fun parseEnv2() {
    val content = BytesUtils.fromString(DATA_ENV_2)

    val environment = envParser.parse(content)

    assertThat(environment).isNotNull
    assertThat(environment.envVersionNumber).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(environment.envApplicationNumber).isEqualTo(1)
    assertThat(environment.envIssuingDate.value).isEqualTo(4031)
    assertThat(environment.envEndDate.value).isEqualTo(6222)
    assertThat(environment.envIssuingDate.getDate()).isEqualTo(LocalDate.of(2021, 1, 14))
    assertThat(environment.envEndDate.getDate()).isEqualTo(LocalDate.of(2027, 1, 14))
    assertThat(environment.holderCompany).isZero
    assertThat(environment.holderIdNumber).isZero
  }

  @Test
  fun generateEnv2() {
    val envIssuingDate = LocalDate.of(2021, 1, 14)
    val envEndDate = LocalDate.of(2027, 1, 14)

    val environment =
        EnvironmentHolderStructure(
            envVersionNumber = VersionNumber.CURRENT_VERSION,
            envApplicationNumber = 1,
            envIssuingDate = DateCompact(envIssuingDate),
            envEndDate = DateCompact(envEndDate),
            holderIdNumber = 0,
            holderCompany = 0)

    assertThat(environment).isNotNull
    assertThat(environment.envVersionNumber).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(environment.envApplicationNumber).isEqualTo(1)
    assertThat(environment.envIssuingDate.value).isEqualTo(4031)
    assertThat(environment.envEndDate.value).isEqualTo(6222)
    assertThat(environment.envIssuingDate.getDate()).isEqualTo(LocalDate.of(2021, 1, 14))
    assertThat(environment.envEndDate.getDate()).isEqualTo(LocalDate.of(2027, 1, 14))
    assertThat(environment.holderCompany).isZero
    assertThat(environment.holderIdNumber).isZero

    val content = EnvironmentHolderStructureParser().generate(environment)

    assertThat(BytesUtils.bytesToString(content)).isEqualTo(DATA_ENV_2)
  }

  companion object {
    private const val DATA_ENV_1 =
        "01 00 00 00 01 0F FB 1C 92 07 00 00 00 08 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"

    private const val DATA_ENV_2 =
        "01 00 00 00 01 0F BF 18 4E 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
  }
}
