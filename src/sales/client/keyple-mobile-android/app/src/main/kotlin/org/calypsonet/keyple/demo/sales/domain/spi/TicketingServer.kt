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
package org.calypsonet.keyple.demo.sales.domain.spi

import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.LoadContractInputDto
import org.calypsonet.keyple.demo.common.dto.LoadContractOutputDto
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardInputDto
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardOutputDto
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Ticketing server: remote services processing the card presented to the local reader (analysis of
 * the contracts, personalization of the card, loading of a contract).
 */
interface TicketingServer {
  fun analyzeContracts(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: AnalyzeContractsInputDto
  ): AnalyzeContractsOutputDto

  fun personalizeCard(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: PersonalizeCardInputDto
  ): PersonalizeCardOutputDto

  fun loadContract(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: LoadContractInputDto
  ): LoadContractOutputDto
}
