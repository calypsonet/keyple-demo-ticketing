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
package org.calypsonet.keyple.demo.sales.server.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.type.VersionNumber;
import org.junit.jupiter.api.Test;

/**
 * Environment written by the card personalization (annex "Card procedures" of the functional
 * documentation, section "Card personalization").
 */
class CardRepositoryTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

  private static CardRepository cardRepository() {
    CardRepository cardRepository = new CardRepository();
    cardRepository.clock =
        Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    return cardRepository;
  }

  @Test
  void environment_isIssuedToday_andValidUntilTheFirstDayOfTheMonthPlus6Years() {
    EnvironmentHolderStructure environment = cardRepository().buildEnvironmentHolder();

    assertThat(environment.getEnvVersionNumber()).isEqualTo(VersionNumber.CURRENT_VERSION);
    assertThat(environment.getEnvIssuingDate().getDate()).isEqualTo(TODAY);
    assertThat(environment.getEnvEndDate().getDate()).isEqualTo(LocalDate.of(2032, 10, 1));
    assertThat(environment.getHolderCompany()).isNull();
    assertThat(environment.getHolderIdNumber()).isNull();
  }

  @Test
  void applicationNumber_isUnique() {
    CardRepository cardRepository = cardRepository();

    int first = cardRepository.buildEnvironmentHolder().getEnvApplicationNumber();
    int second = cardRepository.buildEnvironmentHolder().getEnvApplicationNumber();

    assertThat(second).isEqualTo(first + 1);
  }
}
