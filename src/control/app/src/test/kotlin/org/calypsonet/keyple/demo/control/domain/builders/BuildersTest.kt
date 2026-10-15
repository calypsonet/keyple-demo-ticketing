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
package org.calypsonet.keyple.demo.control.domain.builders

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.reference.Location
import org.junit.Test

/** Contracts and last validation displayed by the control. */
class BuildersTest {

  private val saleDate = LocalDate.of(2026, 10, 1)
  private val validityEndDate = LocalDate.of(2026, 10, 31)
  private val validationDateTime = LocalDateTime.of(2026, 10, 15, 8, 30)

  private val multiTripTicket =
      ContractStructure(
          contractVersionNumber = VersionNumber.CURRENT_VERSION,
          contractTariff = PriorityCode.MULTI_TRIP,
          contractSaleDate = DateCompact(saleDate),
          contractValidityEndDate = DateCompact(validityEndDate),
          contractSaleSam = null,
          contractSaleCounter = null,
          contractAuthKvc = null,
          contractAuthenticator = null)

  @Test
  fun contract_isBuiltFromTheContractReadAndTheResultOfItsControl() {
    val contract =
        buildContract(
            contract = multiTripTicket,
            recordNumber = 2,
            isContractValidated = true,
            isContractExpired = false,
            validationDateTime = validationDateTime,
            remainingTrips = 3)

    assertThat(contract.name).isEqualTo(PriorityCode.MULTI_TRIP.label)
    assertThat(contract.recordNumber).isEqualTo(2)
    assertThat(contract.isValidated).isTrue()
    assertThat(contract.isExpired).isFalse()
    assertThat(contract.validationDateTime).isEqualTo(validationDateTime)
    assertThat(contract.saleDate).isEqualTo(saleDate)
    assertThat(contract.validityEndDate).isEqualTo(validityEndDate)
    assertThat(contract.remainingTrips).isEqualTo(3)
  }

  @Test
  fun validation_isNamedAfterTheContractUsed_atTheLocationOfTheEvent() {
    val locations = listOf(Location(1, "Place de la Bastille"), Location(2, "Gare de Lyon"))

    val validation =
        buildValidation(event = event(location = 2), contract = multiTripTicket, locations)

    assertThat(validation.name).isEqualTo(PriorityCode.MULTI_TRIP.label)
    assertThat(validation.location).isEqualTo(Location(2, "Gare de Lyon"))
    assertThat(validation.dateTime).isEqualTo(validationDateTime)
  }

  @Test
  fun validation_withoutContract_isNamedEvent() {
    val validation =
        buildValidation(
            event = event(location = 1),
            contract = null,
            locations = listOf(Location(1, "Place de la Bastille")))

    assertThat(validation.name).isEqualTo("Event")
  }

  private fun event(location: Int): EventStructure =
      EventStructure(
          eventVersionNumber = VersionNumber.CURRENT_VERSION,
          eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
          eventTimeStamp = TimeCompact(validationDateTime),
          eventLocation = location,
          eventContractUsed = 1,
          contractPriorities =
              listOf(
                  PriorityCode.MULTI_TRIP,
                  PriorityCode.FORBIDDEN,
                  PriorityCode.FORBIDDEN,
                  PriorityCode.FORBIDDEN))
}
