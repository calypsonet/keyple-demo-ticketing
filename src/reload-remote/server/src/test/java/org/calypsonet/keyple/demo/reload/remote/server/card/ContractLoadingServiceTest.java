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
package org.calypsonet.keyple.demo.reload.remote.server.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.EventStructure;
import org.calypsonet.keyple.demo.common.model.type.DateCompact;
import org.calypsonet.keyple.demo.common.model.type.PriorityCode;
import org.calypsonet.keyple.demo.common.model.type.TimeCompact;
import org.calypsonet.keyple.demo.common.model.type.VersionNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Business rules of the contract loading (data model of the demo, Distribution/Loading process).
 */
class ContractLoadingServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
  private static final LocalDate ENV_END_DATE = LocalDate.of(2032, 10, 1);
  private static final int MAX_COUNTER_VALUE = 0xFFFFFF;

  private ContractLoadingService service;

  @BeforeEach
  void setUp() {
    service = new ContractLoadingService();
    service.clock =
        Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
  }

  // ========== Contracts of the same type ==========

  @Test
  void newMultiTripTicket_isWrittenInTheFirstEmptyRecord() {
    CardData card = card(priorities(), blank(), blank(), blank(), blank());

    service.insertNewContract(PriorityCode.MULTI_TRIP, 3, card);

    ContractStructure contract = card.getContracts().get(0);
    assertThat(contract.getContractVersionNumber()).isEqualTo(VersionNumber.CURRENT_VERSION);
    assertThat(contract.getContractTariff()).isEqualTo(PriorityCode.MULTI_TRIP);
    assertThat(contract.getContractSaleDate().getDate()).isEqualTo(TODAY);
    assertThat(contract.getContractValidityEndDate().getDate()).isEqualTo(ENV_END_DATE);
    assertThat(contract.getCounterValue()).isEqualTo(3);
    assertThat(card.getEvent().getContractPriority(1)).isEqualTo(PriorityCode.MULTI_TRIP);
    assertThat(card.isEventUpdated()).isTrue();
  }

  @Test
  void tripsAreAddedToTheExistingMultiTripTicket() {
    CardData card =
        card(
            priorities(PriorityCode.SEASON_PASS, PriorityCode.MULTI_TRIP),
            seasonPass(TODAY.plusDays(10)),
            multiTrip(2),
            blank(),
            blank());

    service.insertNewContract(PriorityCode.MULTI_TRIP, 3, card);

    assertThat(card.getContracts().get(1).getCounterValue()).isEqualTo(5);
    assertThat(card.getContracts().get(2)).isEqualTo(blank());
  }

  @Test
  void exhaustedMultiTripTicket_isReloaded() {
    CardData card = card(priorities(PriorityCode.EXPIRED), multiTrip(0), blank(), blank(), blank());

    service.insertNewContract(PriorityCode.MULTI_TRIP, 2, card);

    assertThat(card.getContracts().get(0).getCounterValue()).isEqualTo(2);
    assertThat(card.getEvent().getContractPriority(1)).isEqualTo(PriorityCode.MULTI_TRIP);
  }

  @Test
  void newSeasonPass_isValidFor30Days() {
    CardData card = card(priorities(), blank(), blank(), blank(), blank());

    service.insertNewContract(PriorityCode.SEASON_PASS, 0, card);

    ContractStructure contract = card.getContracts().get(0);
    assertThat(contract.getContractTariff()).isEqualTo(PriorityCode.SEASON_PASS);
    assertThat(contract.getContractValidityEndDate().getDate()).isEqualTo(TODAY.plusDays(30));
    assertThat(contract.getCounterValue()).isNull();
  }

  @Test
  void renewedSeasonPass_endingToday_isExtended() {
    CardData card =
        card(priorities(PriorityCode.SEASON_PASS), seasonPass(TODAY), blank(), blank(), blank());

    service.insertNewContract(PriorityCode.SEASON_PASS, 0, card);

    assertThat(card.getContracts().get(0).getContractValidityEndDate().getDate())
        .isEqualTo(TODAY.plusDays(30));
  }

  @Test
  void renewedSeasonPass_isExtendedFromItsEndDate_whenNotExpired() {
    CardData card =
        card(
            priorities(PriorityCode.SEASON_PASS),
            seasonPass(TODAY.plusDays(10)),
            blank(),
            blank(),
            blank());

    service.insertNewContract(PriorityCode.SEASON_PASS, 0, card);

    assertThat(card.getContracts().get(0).getContractValidityEndDate().getDate())
        .isEqualTo(TODAY.plusDays(40));
  }

  @Test
  void renewedSeasonPass_isValidFromToday_whenExpired() {
    CardData card =
        card(
            priorities(PriorityCode.SEASON_PASS),
            seasonPass(TODAY.minusDays(5)),
            blank(),
            blank(),
            blank());

    service.insertNewContract(PriorityCode.SEASON_PASS, 0, card);

    assertThat(card.getContracts().get(0).getContractValidityEndDate().getDate())
        .isEqualTo(TODAY.plusDays(30));
    assertThat(card.getEvent().getContractPriority(1)).isEqualTo(PriorityCode.SEASON_PASS);
  }

  // ========== Position of a new contract ==========

  @Test
  void newContract_replacesTheFirstExpiredContract_whenNoRecordIsEmpty() {
    CardData card =
        card(
            priorities(
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS),
            seasonPass(TODAY.plusDays(5)),
            seasonPass(TODAY.minusDays(1)),
            seasonPass(TODAY.minusDays(2)),
            seasonPass(TODAY.plusDays(5)));

    service.insertNewContract(PriorityCode.MULTI_TRIP, 1, card);

    assertThat(card.getContracts().get(1).getContractTariff()).isEqualTo(PriorityCode.MULTI_TRIP);
    assertThat(card.getEvent().getContractPriorities())
        .containsExactly(
            PriorityCode.SEASON_PASS,
            PriorityCode.MULTI_TRIP,
            PriorityCode.EXPIRED,
            PriorityCode.SEASON_PASS);
  }

  @Test
  void emptyRecordWithAnUnexpectedPriority_isUsedForANewContract() {
    // ContractPriority not reset to 0 for an empty record: set to 0 by the contract analysis
    CardData card =
        card(
            priorities(
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS),
            seasonPass(TODAY.plusDays(5)),
            blank(),
            seasonPass(TODAY.plusDays(5)),
            seasonPass(TODAY.plusDays(5)));

    service.insertNewContract(PriorityCode.MULTI_TRIP, 1, card);

    assertThat(card.getContracts().get(1).getContractTariff()).isEqualTo(PriorityCode.MULTI_TRIP);
  }

  @Test
  void cardWithoutEmptyNorExpiredRecord_isRejected() {
    CardData card =
        card(
            priorities(
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS,
                PriorityCode.SEASON_PASS),
            seasonPass(TODAY.plusDays(5)),
            seasonPass(TODAY.plusDays(5)),
            seasonPass(TODAY.plusDays(5)),
            seasonPass(TODAY.plusDays(5)));

    assertThatThrownBy(() -> service.insertNewContract(PriorityCode.MULTI_TRIP, 1, card))
        .isInstanceOf(CardFullException.class);
  }

  // ========== Single contract cards ==========

  @Test
  void singleContractCard_contractOfAnotherType_isReplaced_whenExhausted() {
    CardData card = card(priorities(PriorityCode.EXPIRED), multiTrip(0));

    service.insertNewContract(PriorityCode.SEASON_PASS, 0, card);

    assertThat(card.getContracts().get(0).getContractTariff()).isEqualTo(PriorityCode.SEASON_PASS);
  }

  @Test
  void singleContractCard_contractOfAnotherType_isReplaced_whenExpired() {
    CardData card = card(priorities(PriorityCode.SEASON_PASS), seasonPass(TODAY.minusDays(1)));

    service.insertNewContract(PriorityCode.MULTI_TRIP, 2, card);

    assertThat(card.getContracts().get(0).getContractTariff()).isEqualTo(PriorityCode.MULTI_TRIP);
    assertThat(card.getContracts().get(0).getCounterValue()).isEqualTo(2);
  }

  @Test
  void singleContractCard_contractOfAnotherType_isNotReplaced_whenStillValid() {
    ContractStructure multiTrip = multiTrip(3);
    CardData card = card(priorities(PriorityCode.MULTI_TRIP), multiTrip);

    assertThatThrownBy(() -> service.insertNewContract(PriorityCode.SEASON_PASS, 0, card))
        .isInstanceOf(CardFullException.class);
    assertThat(card.getContracts().get(0)).isEqualTo(multiTrip);
  }

  // ========== Input data ==========

  @Test
  void contractOtherThanSeasonPassOrMultiTripTicket_isRejected() {
    CardData card = card(priorities(), blank(), blank(), blank(), blank());

    assertThatThrownBy(() -> service.insertNewContract(PriorityCode.EXPIRED, 1, card))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void numberOfTripsToLoad_mustBePositive() {
    for (Integer tripsToLoad : Arrays.asList(null, 0, -1)) {
      CardData card = card(priorities(), blank(), blank(), blank(), blank());

      assertThatThrownBy(
              () -> service.insertNewContract(PriorityCode.MULTI_TRIP, tripsToLoad, card))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void counter_cannotExceedItsCapacity() {
    CardData card =
        card(
            priorities(PriorityCode.MULTI_TRIP),
            multiTrip(MAX_COUNTER_VALUE - 1),
            blank(),
            blank(),
            blank());

    service.insertNewContract(PriorityCode.MULTI_TRIP, 1, card);
    assertThat(card.getContracts().get(0).getCounterValue()).isEqualTo(MAX_COUNTER_VALUE);

    assertThatThrownBy(() -> service.insertNewContract(PriorityCode.MULTI_TRIP, 1, card))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ========== Test data ==========

  private static CardData card(List<PriorityCode> priorities, ContractStructure... contracts) {
    EnvironmentHolderStructure environment =
        new EnvironmentHolderStructure(
            VersionNumber.CURRENT_VERSION,
            1,
            new DateCompact(TODAY.minusYears(1)),
            new DateCompact(ENV_END_DATE),
            null,
            null);
    EventStructure event =
        new EventStructure(
            VersionNumber.CURRENT_VERSION,
            new DateCompact(TODAY.minusDays(1)),
            new TimeCompact(TODAY.minusDays(1).atTime(8, 30)),
            1,
            1,
            priorities);
    return new CardData(environment, new ArrayList<>(Arrays.asList(contracts)), event);
  }

  /** Returns the 4 priorities of the event, the missing ones being 0 (FORBIDDEN). */
  private static List<PriorityCode> priorities(PriorityCode... priorities) {
    List<PriorityCode> list =
        new ArrayList<>(Collections.nCopies(EventStructure.CONTRACT_COUNT, PriorityCode.FORBIDDEN));
    for (int i = 0; i < priorities.length; i++) {
      list.set(i, priorities[i]);
    }
    return list;
  }

  private static ContractStructure blank() {
    return new ContractStructure(
        VersionNumber.UNDEFINED,
        PriorityCode.FORBIDDEN,
        new DateCompact(0),
        new DateCompact(0),
        null,
        null,
        null,
        null);
  }

  private static ContractStructure seasonPass(LocalDate validityEndDate) {
    return new ContractStructure(
        VersionNumber.CURRENT_VERSION,
        PriorityCode.SEASON_PASS,
        new DateCompact(validityEndDate.minusDays(30)),
        new DateCompact(validityEndDate),
        null,
        null,
        null,
        null);
  }

  private static ContractStructure multiTrip(int counterValue) {
    return new ContractStructure(
        VersionNumber.CURRENT_VERSION,
        PriorityCode.MULTI_TRIP,
        new DateCompact(TODAY.minusDays(20)),
        new DateCompact(ENV_END_DATE),
        null,
        null,
        null,
        null,
        counterValue);
  }
}
