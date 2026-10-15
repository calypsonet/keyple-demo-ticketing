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
package org.calypsonet.keyple.demo.control.domain.procedures

import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.codecs.CalypsoContractCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEventCodec
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.reference.Location
import org.calypsonet.keyple.demo.control.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.model.RejectionReason
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.card.ElementaryFile
import org.eclipse.keypop.calypso.card.card.FileData
import org.eclipse.keypop.calypso.card.transaction.FreeTransactionManager
import org.eclipse.keypop.reader.ChannelControl
import org.junit.Test

/**
 * Control procedure of the Calypso cards without SAM (reading without authentication), the card and
 * its transaction being simulated.
 */
class CalypsoCardControlProcedureTest {

  private val controlLocation = Location(1, "Place de la Bastille")
  private val locations = listOf(controlLocation, Location(2, "Gare de Lyon"))
  private val controlDateTime = LocalDateTime.of(2026, 10, 15, 8, 35, 20)
  private val today = controlDateTime.toLocalDate()

  /** Validation 5 minutes before the control, within the validation period (10 minutes). */
  private val recentValidation = LocalDateTime.of(2026, 10, 15, 8, 30)

  @Test
  fun contractUsedByARecentValidation_atTheLocationOfTheControl_isValidated() {
    val result =
        control(
            event(recentValidation, location = 1, contractUsed = 1),
            contracts = mapOf(1 to multiTripTicket(), 2 to seasonPass(today.plusDays(10))),
            counters = mapOf(1 to 3))
            as ControlResult.CardContent

    assertThat(result.authenticationMode).isEqualTo(AuthenticationMode.NO_AUTHENTICATION)
    assertThat(result.contracts).hasSize(2)
    val multiTripTicket = result.contracts.first { it.recordNumber == 1 }
    assertThat(multiTripTicket.isValidated).isTrue()
    assertThat(multiTripTicket.validationDateTime).isEqualTo(recentValidation)
    assertThat(multiTripTicket.remainingTrips).isEqualTo(3)
    assertThat(result.contracts.first { it.recordNumber == 2 }.isValidated).isFalse()
    assertThat(result.lastValidation!!.location).isEqualTo(controlLocation)
  }

  @Test
  fun contractUsedByAValidationAtAnotherLocation_isNotValidated_theValidationBeingDisplayed() {
    val result =
        control(
            event(recentValidation, location = 2, contractUsed = 1),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 3))
            as ControlResult.CardContent

    assertThat(result.contracts.single().isValidated).isFalse()
    assertThat(result.lastValidation!!.location).isEqualTo(Location(2, "Gare de Lyon"))
  }

  @Test
  fun expiredContract_isMarkedExpired() {
    val result =
        control(
            event(recentValidation, location = 1, contractUsed = 2),
            contracts = mapOf(1 to seasonPass(today.minusDays(1))))
            as ControlResult.CardContent

    assertThat(result.contracts.single().isExpired).isTrue()
  }

  @Test
  fun emptyRecords_areNotDisplayed() {
    val result =
        control(
            event(recentValidation, location = 1, contractUsed = 1),
            contracts = mapOf(1 to multiTripTicket(), 2 to emptyRecord()),
            counters = mapOf(1 to 3))
            as ControlResult.CardContent

    assertThat(result.contracts.map { it.recordNumber }).containsExactly(1)
  }

  @Test
  fun contractOfAnUnexpectedVersion_isRefused() {
    val result =
        control(
            event(recentValidation, location = 1, contractUsed = 1),
            contracts =
                mapOf(
                    1 to multiTripTicket(),
                    2 to
                        seasonPass(today.plusDays(10))
                            .copy(contractVersionNumber = VersionNumber.RESERVED)),
            counters = mapOf(1 to 3))

    assertThat(result).isEqualTo(ControlResult.Rejected(RejectionReason.CONTRACT_WRONG_VERSION))
  }

  @Test
  fun cardNeverLoaded_isAnEmptyCard() {
    val result =
        control(
            event(recentValidation, location = 1, contractUsed = 0)
                .copy(eventVersionNumber = VersionNumber.UNDEFINED),
            contracts = emptyMap())

    assertThat(result).isEqualTo(ControlResult.EmptyCard)
  }

  // ========== Simulated card ==========

  private fun control(
      event: EventStructure,
      contracts: Map<Int, ContractStructure>,
      counters: Map<Int, Int> = emptyMap()
  ): ControlResult {
    val environment =
        EnvironmentHolderStructure(
            envVersionNumber = VersionNumber.CURRENT_VERSION,
            envApplicationNumber = 1,
            envIssuingDate = DateCompact(today.minusYears(1)),
            envEndDate = DateCompact(today.plusYears(5)),
            holderCompany = null,
            holderIdNumber = null)
    val countersData = mockk<FileData>()
    counters.forEach { (number, value) ->
      every { countersData.getContentAsCounterValue(number) } returns value
    }
    val contractsData = mockk<FileData>()
    every { contractsData.allRecordsContent } returns
        contracts.mapValues { CalypsoContractCodec.encode(it.value) }.toSortedMap()

    val calypsoCard = mockk<CalypsoCard>()
    every { calypsoCard.isPkiModeSupported } returns false
    every { calypsoCard.productType } returns CalypsoCard.ProductType.PRIME_REVISION_3
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER) } returns
        file(content(CalypsoEnvironmentHolderCodec.encode(environment)))
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENT_LOG) } returns
        file(content(CalypsoEventCodec.encode(event)))
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS) } returns file(contractsData)
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_COUNTERS) } returns file(countersData)

    val transaction = mockk<FreeTransactionManager>()
    every { transaction.prepareReadRecords(any(), any(), any(), any()) } returns transaction
    every { transaction.prepareReadCounter(any(), any()) } returns transaction
    every { transaction.processCommands(any<ChannelControl>()) } returns transaction

    val keypopApiProvider = mockk<KeypopApiProvider>()
    every {
      keypopApiProvider.getCalypsoCardApiFactory().createFreeTransactionManager(any(), any())
    } returns transaction

    return CalypsoCardControlProcedure(keypopApiProvider, mockk(relaxed = true))
        .execute(
            ControlContext(
                cardReader = mockk(),
                card = calypsoCard,
                dateTime = controlDateTime,
                location = controlLocation,
                locations = locations,
                validationPeriodMinutes = 10,
                symmetricCryptoSecuritySetting = null,
                asymmetricCryptoSecuritySetting = mockk()))
  }

  private fun content(bytes: ByteArray): FileData {
    val data = mockk<FileData>()
    every { data.content } returns bytes
    return data
  }

  private fun file(data: FileData): ElementaryFile {
    val file = mockk<ElementaryFile>()
    every { file.data } returns data
    return file
  }

  private fun event(dateTime: LocalDateTime, location: Int, contractUsed: Int): EventStructure =
      EventStructure(
          eventVersionNumber = VersionNumber.CURRENT_VERSION,
          eventDateStamp = DateCompact(dateTime.toLocalDate()),
          eventTimeStamp = TimeCompact(dateTime),
          eventLocation = location,
          eventContractUsed = contractUsed,
          contractPriorities = List(EventStructure.CONTRACT_COUNT) { PriorityCode.FORBIDDEN })

  private fun emptyRecord(): ContractStructure =
      ContractStructure(
          contractVersionNumber = VersionNumber.UNDEFINED,
          contractTariff = PriorityCode.FORBIDDEN,
          contractSaleDate = DateCompact(0),
          contractValidityEndDate = DateCompact(0),
          contractSaleSam = null,
          contractSaleCounter = null,
          contractAuthKvc = null,
          contractAuthenticator = null)

  private fun multiTripTicket(): ContractStructure =
      ContractStructure(
          contractVersionNumber = VersionNumber.CURRENT_VERSION,
          contractTariff = PriorityCode.MULTI_TRIP,
          contractSaleDate = DateCompact(today.minusDays(20)),
          contractValidityEndDate = DateCompact(today.plusYears(5)),
          contractSaleSam = null,
          contractSaleCounter = null,
          contractAuthKvc = null,
          contractAuthenticator = null)

  private fun seasonPass(validityEndDate: LocalDate): ContractStructure =
      ContractStructure(
          contractVersionNumber = VersionNumber.CURRENT_VERSION,
          contractTariff = PriorityCode.SEASON_PASS,
          contractSaleDate = DateCompact(validityEndDate.minusDays(30)),
          contractValidityEndDate = DateCompact(validityEndDate),
          contractSaleSam = null,
          contractSaleCounter = null,
          contractAuthKvc = null,
          contractAuthenticator = null)
}
