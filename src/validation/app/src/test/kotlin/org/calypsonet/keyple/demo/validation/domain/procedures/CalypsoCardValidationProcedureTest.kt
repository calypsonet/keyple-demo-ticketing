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
package org.calypsonet.keyple.demo.validation.domain.procedures

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.card.ElementaryFile
import org.eclipse.keypop.calypso.card.card.FileData
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.reader.ChannelControl
import org.junit.Test

/**
 * Validation procedure of the Calypso cards, the card and its transaction being simulated: data
 * read, counter decreased, and event written.
 */
class CalypsoCardValidationProcedureTest {

  private val location = Location(1, "Place de la Bastille")
  private val validationDateTime = LocalDateTime.of(2026, 10, 15, 8, 30, 40)
  private val today = validationDateTime.toLocalDate()

  private val transaction = mockk<SecureRegularModeTransactionManager>(relaxed = true)
  private val writtenEvent = slot<ByteArray>()

  init {
    every { transaction.prepareOpenSecureSession(any()) } returns transaction
    every { transaction.prepareReadRecords(any(), any(), any(), any()) } returns transaction
    every { transaction.prepareReadCounter(any(), any()) } returns transaction
    every { transaction.prepareDecreaseCounter(any(), any(), any()) } returns transaction
    every {
      transaction.prepareUpdateRecord(CalypsoFiles.SFI_EVENT_LOG, 1, capture(writtenEvent))
    } returns transaction
    every { transaction.prepareCloseSecureSession() } returns transaction
    every { transaction.prepareCancelSecureSession() } returns transaction
    every { transaction.processCommands(any<ChannelControl>()) } returns transaction
  }

  // ========== Accepted cards ==========

  @Test
  fun multiTripTicket_isValidated_andDecremented() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.MULTI_TRIP)),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 3))

    assertThat(result).isInstanceOf(ValidationResult.Accepted::class.java)
    assertThat((result as ValidationResult.Accepted).remainingTrips).isEqualTo(2)
    verify { transaction.prepareDecreaseCounter(CalypsoFiles.SFI_COUNTERS, 1, 1) }
    verify { transaction.prepareCloseSecureSession() }

    val event = CalypsoEventCodec.decode(writtenEvent.captured)
    assertThat(event.eventDateTime).isEqualTo(LocalDateTime.of(2026, 10, 15, 8, 30))
    assertThat(event.eventLocation).isEqualTo(location.id)
    assertThat(event.eventContractUsed).isEqualTo(1)
    assertThat(event.getContractPriority(1)).isEqualTo(PriorityCode.MULTI_TRIP)
  }

  @Test
  fun seasonPass_isValidated_withoutDecrement() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.SEASON_PASS)),
            contracts = mapOf(1 to seasonPass(today.plusDays(10))))

    assertThat(result).isInstanceOf(ValidationResult.Accepted::class.java)
    assertThat((result as ValidationResult.Accepted).seasonPassValidityEndDate)
        .isEqualTo(today.plusDays(10))
    verify(exactly = 0) { transaction.prepareDecreaseCounter(any(), any(), any()) }
  }

  @Test
  fun contractType_isItsTariff_notItsPriority() {
    // Priority of a season pass, tariff of a multi-trip ticket: the contract is a multi-trip ticket
    val result =
        validate(
            event(priorities = listOf(PriorityCode.SEASON_PASS)),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 3))

    assertThat((result as ValidationResult.Accepted).remainingTrips).isEqualTo(2)
    verify { transaction.prepareDecreaseCounter(CalypsoFiles.SFI_COUNTERS, 1, 1) }
  }

  @Test
  fun seasonPass_isUsedBeforeTheMultiTripTicket() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.MULTI_TRIP, PriorityCode.SEASON_PASS)),
            contracts = mapOf(1 to multiTripTicket(), 2 to seasonPass(today.plusDays(10))),
            counters = mapOf(1 to 3))

    assertThat(result).isInstanceOf(ValidationResult.Accepted::class.java)
    assertThat(CalypsoEventCodec.decode(writtenEvent.captured).eventContractUsed).isEqualTo(2)
    verify(exactly = 0) { transaction.prepareDecreaseCounter(any(), any(), any()) }
  }

  // ========== Rejected cards ==========

  @Test
  fun expiredSeasonPass_isRejected_andMarkedExpiredInTheEvent() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.SEASON_PASS)),
            contracts = mapOf(1 to seasonPass(today.minusDays(1))))

    assertThat(result)
        .isEqualTo(
            ValidationResult.Rejected(result.cardDescription!!, RejectionReason.EXPIRED_CONTRACT))
    // The event is written with the updated priority, so that the contract is not read again
    verify { transaction.prepareCloseSecureSession() }
    assertThat(CalypsoEventCodec.decode(writtenEvent.captured).getContractPriority(1))
        .isEqualTo(PriorityCode.EXPIRED)
  }

  @Test
  fun exhaustedMultiTripTicket_isRejected_andMarkedExpiredInTheEvent() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.MULTI_TRIP)),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 0))

    assertThat((result as ValidationResult.Rejected).reason)
        .isEqualTo(RejectionReason.NO_TRIPS_LEFT)
    assertThat(CalypsoEventCodec.decode(writtenEvent.captured).getContractPriority(1))
        .isEqualTo(PriorityCode.EXPIRED)
  }

  @Test
  fun cardNeverLoaded_isRejected_withoutWriting() {
    val result =
        validate(
            event(priorities = emptyList()).copy(eventVersionNumber = VersionNumber.UNDEFINED),
            contracts = emptyMap())

    assertThat((result as ValidationResult.Rejected).reason)
        .isEqualTo(RejectionReason.NO_VALID_CONTRACT)
    verify { transaction.prepareCancelSecureSession() }
    assertThat(writtenEvent.isCaptured).isFalse()
  }

  @Test
  fun expiredEnvironment_isRejected() {
    val result =
        validate(
            event(priorities = listOf(PriorityCode.SEASON_PASS)),
            contracts = mapOf(1 to seasonPass(today.plusDays(10))),
            environmentEndDate = today.minusDays(1))

    assertThat((result as ValidationResult.Rejected).reason)
        .isEqualTo(RejectionReason.ENVIRONMENT_EXPIRED)
    verify { transaction.prepareCancelSecureSession() }
  }

  // ========== Anti-passback ==========

  @Test
  fun secondValidationInTheSameMinute_isRejected() {
    val result =
        validate(
            event(
                priorities = listOf(PriorityCode.MULTI_TRIP),
                dateTime = LocalDateTime.of(2026, 10, 15, 8, 30)),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 3),
            isDfRatified = true)

    assertThat((result as ValidationResult.Rejected).reason)
        .isEqualTo(RejectionReason.ALREADY_VALIDATED)
    verify(exactly = 0) { transaction.prepareDecreaseCounter(any(), any(), any()) }
    verify { transaction.prepareCancelSecureSession() }
  }

  @Test
  fun validationInterruptedByTheRemovalOfTheCard_isConsideredValidated() {
    // Previous session not ratified: the card has been removed before the end of the validation
    val result =
        validate(
            event(
                priorities = listOf(PriorityCode.MULTI_TRIP),
                dateTime = LocalDateTime.of(2026, 10, 15, 8, 30)),
            contracts = mapOf(1 to multiTripTicket()),
            counters = mapOf(1 to 3),
            isDfRatified = false)

    assertThat(result).isInstanceOf(ValidationResult.Accepted::class.java)
    verify(exactly = 0) { transaction.prepareDecreaseCounter(any(), any(), any()) }
    verify { transaction.prepareCloseSecureSession() }
  }

  // ========== Simulated card ==========

  private fun validate(
      event: EventStructure,
      contracts: Map<Int, ContractStructure>,
      counters: Map<Int, Int> = emptyMap(),
      environmentEndDate: LocalDate = today.plusYears(5),
      isDfRatified: Boolean = true
  ): ValidationResult {
    val environment =
        EnvironmentHolderStructure(
            envVersionNumber = VersionNumber.CURRENT_VERSION,
            envApplicationNumber = 1,
            envIssuingDate = DateCompact(today.minusYears(1)),
            envEndDate = DateCompact(environmentEndDate),
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
    every { calypsoCard.dfName } returns byteArrayOf(0x31, 0x54)
    every { calypsoCard.productType } returns CalypsoCard.ProductType.PRIME_REVISION_3
    every { calypsoCard.isDfRatified } returns isDfRatified
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER) } returns
        file(content(CalypsoEnvironmentHolderCodec.encode(environment)))
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENT_LOG) } returns
        file(content(CalypsoEventCodec.encode(event)))
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS) } returns file(contractsData)
    every { calypsoCard.getFileBySfi(CalypsoFiles.SFI_COUNTERS) } returns file(countersData)

    val keypopApiProvider = mockk<KeypopApiProvider>()
    every {
      keypopApiProvider
          .getCalypsoCardApiFactory()
          .createSecureRegularModeTransactionManager(any(), any(), any())
    } returns transaction

    return CalypsoCardValidationProcedure(keypopApiProvider, mockk(relaxed = true))
        .execute(
            ValidationContext(
                cardReader = mockk(),
                card = calypsoCard,
                dateTime = validationDateTime,
                location = location,
                cardSecuritySetting = mockk()))
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

  /** Last event (validation of the day before by default), the missing priorities being 0. */
  private fun event(
      priorities: List<PriorityCode>,
      dateTime: LocalDateTime = LocalDateTime.of(2026, 10, 14, 18, 0)
  ): EventStructure =
      EventStructure(
          eventVersionNumber = VersionNumber.CURRENT_VERSION,
          eventDateStamp = DateCompact(dateTime.toLocalDate()),
          eventTimeStamp = TimeCompact(dateTime),
          eventLocation = location.id,
          eventContractUsed = 1,
          contractPriorities =
              priorities +
                  List(EventStructure.CONTRACT_COUNT - priorities.size) { PriorityCode.FORBIDDEN })

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
