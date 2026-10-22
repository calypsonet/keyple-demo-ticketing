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

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.junit.jupiter.api.Test

/** Codecs of the records of the storage cards (16 bytes): encoding then decoding. */
class StorageCardCodecsTest {

  @Test
  fun environment_isEncodedIn16Bytes_andDecodedUnchanged() {
    val environment =
        EnvironmentHolderStructure(
            envVersionNumber = VersionNumber.CURRENT_VERSION,
            envApplicationNumber = 123456789,
            envIssuingDate = DateCompact(LocalDate.of(2026, 10, 15)),
            envEndDate = DateCompact(LocalDate.of(2032, 10, 1)),
            holderCompany = 7,
            holderIdNumber = 42)

    val content = StorageCardEnvironmentHolderCodec.encode(environment)

    assertThat(content).hasSize(16)
    assertThat(StorageCardEnvironmentHolderCodec.decode(content)).isEqualTo(environment)
  }

  @Test
  fun contract_isEncodedIn16Bytes_withItsCounter_andDecodedUnchanged() {
    val contract =
        ContractStructure(
            contractVersionNumber = VersionNumber.CURRENT_VERSION,
            contractTariff = PriorityCode.MULTI_TRIP,
            contractSaleDate = DateCompact(LocalDate.of(2026, 10, 15)),
            contractValidityEndDate = DateCompact(LocalDate.of(2032, 10, 1)),
            contractSaleSam = null,
            contractSaleCounter = null,
            contractAuthKvc = 0,
            contractAuthenticator = 0,
            counterValue = 0xFFFFFF)

    val content = StorageCardContractCodec.encode(contract)

    assertThat(content).hasSize(16)
    assertThat(StorageCardContractCodec.decode(content)).isEqualTo(contract)
  }

  @Test
  fun event_isEncodedIn16Bytes_andDecodedUnchanged() {
    val eventDateTime = LocalDateTime.of(2026, 10, 15, 8, 30)
    val event =
        EventStructure(
            eventVersionNumber = VersionNumber.CURRENT_VERSION,
            eventDateStamp = DateCompact(eventDateTime.toLocalDate()),
            eventTimeStamp = TimeCompact(eventDateTime),
            eventLocation = 3,
            eventContractUsed = 1,
            contractPriorities =
                listOf(
                    PriorityCode.MULTI_TRIP,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN))

    val content = StorageCardEventCodec.encode(event)

    assertThat(content).hasSize(16)
    assertThat(StorageCardEventCodec.decode(content)).isEqualTo(event)
  }

  @Test
  fun emptyRecords_areDecodedAsUndefinedStructures() {
    val emptyRecord = ByteArray(16)

    assertThat(StorageCardEnvironmentHolderCodec.decode(emptyRecord).envVersionNumber)
        .isEqualTo(VersionNumber.UNDEFINED)
    assertThat(StorageCardContractCodec.decode(emptyRecord).contractTariff)
        .isEqualTo(PriorityCode.FORBIDDEN)
    assertThat(StorageCardEventCodec.decode(emptyRecord).contractPriorities)
        .containsOnly(PriorityCode.FORBIDDEN)
  }
}
