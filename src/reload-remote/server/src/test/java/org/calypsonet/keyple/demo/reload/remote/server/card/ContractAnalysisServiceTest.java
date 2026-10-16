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
 * Business rules of the contract analysis (data model of the demo, Distribution/Loading process).
 */
class ContractAnalysisServiceTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
  private static final LocalDate ENV_END_DATE = LocalDate.of(2032, 10, 1);

  private ContractAnalysisService service;

  @BeforeEach
  void setUp() {
    service = new ContractAnalysisService();
    service.clock =
        Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
  }

  @Test
  void presentContracts_areTheContractsOfTheCurrentVersion() {
    CardData card = card(VersionNumber.CURRENT_VERSION, multiTrip(), blank(), blank(), blank());

    assertThat(service.findPresentContracts(card))
        .extracting(ContractStructure::getContractTariff)
        .containsExactly(PriorityCode.MULTI_TRIP);
  }

  @Test
  void cleanCard_hasNoContract() {
    CardData card = card(VersionNumber.UNDEFINED, blank(), blank(), blank(), blank());

    assertThat(service.findPresentContracts(card)).isEmpty();
  }

  @Test
  void eventOfAnUnexpectedVersion_isRejected() {
    CardData card = card(VersionNumber.RESERVED, multiTrip(), blank(), blank(), blank());

    assertThatThrownBy(() -> service.findPresentContracts(card))
        .isInstanceOf(EventWrongVersionException.class);
  }

  // ========== Card data ==========

  private static CardData card(VersionNumber eventVersionNumber, ContractStructure... contracts) {
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
            eventVersionNumber,
            new DateCompact(TODAY.minusDays(1)),
            new TimeCompact(TODAY.minusDays(1).atTime(8, 30)),
            1,
            1,
            new ArrayList<>(
                Collections.nCopies(EventStructure.CONTRACT_COUNT, PriorityCode.FORBIDDEN)));
    return new CardData(environment, new ArrayList<>(Arrays.asList(contracts)), event);
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

  private static ContractStructure multiTrip() {
    return new ContractStructure(
        VersionNumber.CURRENT_VERSION,
        PriorityCode.MULTI_TRIP,
        new DateCompact(TODAY.minusDays(20)),
        new DateCompact(ENV_END_DATE),
        null,
        null,
        null,
        null,
        5);
  }
}
