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
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.junit.jupiter.api.Test

class ContractInfoStructureParserTest {

  private val contractStructureParser = ContractStructureParser()

  @Test
  fun parseContract1() {
    val content = BytesUtils.fromString(DATA_CONTRACT_1)

    val contract = contractStructureParser.parse(content)

    assertThat(contract).isNotNull
    assertThat(contract.contractVersionNumber).isEqualTo(VersionNumber.CURRENT_VERSION)
    assertThat(contract.contractTariff).isEqualTo(PriorityCode.SEASON_PASS)
    assertThat(contract.contractSaleDate.value).isEqualTo(4031)
    assertThat(contract.contractValidityEndDate.value).isEqualTo(4061)
    assertThat(contract.contractSaleDate.getDate()).isEqualTo(LocalDate.of(2021, 1, 14))
    assertThat(contract.contractValidityEndDate.getDate()).isEqualTo(LocalDate.of(2021, 2, 13))
    assertThat(contract.contractSaleSam).isZero
    assertThat(contract.contractSaleCounter).isZero
    assertThat(contract.contractAuthKvc).isZero
    assertThat(contract.contractAuthenticator).isZero
  }

  @Test
  fun generateContract1() {
    val contractSaleDate = LocalDate.of(2021, 1, 14)
    val contractValidityEndDate = LocalDate.of(2021, 2, 13)

    val contractStructure =
        ContractStructure(
            contractVersionNumber = VersionNumber.CURRENT_VERSION,
            contractTariff = PriorityCode.SEASON_PASS,
            contractSaleDate = DateCompact(contractSaleDate),
            contractValidityEndDate = DateCompact(contractValidityEndDate),
            contractSaleSam = 0,
            contractSaleCounter = 0,
            contractAuthKvc = 0,
            contractAuthenticator = 0)

    val content = ContractStructureParser().generate(contractStructure)

    assertThat(BytesUtils.bytesToString(content)).isEqualTo(DATA_CONTRACT_1)
  }

  companion object {
    private const val DATA_CONTRACT_1 =
        "01 01 0F BF 0F DD 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
  }
}
