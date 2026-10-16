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
package org.calypsonet.keyple.demo.reload.remote.data.server

import javax.inject.Inject
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.LoadContractInputDto
import org.calypsonet.keyple.demo.common.dto.LoadContractOutputDto
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardInputDto
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardOutputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceId
import org.calypsonet.keyple.demo.reload.remote.domain.spi.TicketingServer
import org.eclipse.keyple.distributed.LocalServiceClient
import org.eclipse.keypop.reader.selection.spi.SmartCard

class KeypleTicketingServer
@Inject
constructor(private val localServiceClient: LocalServiceClient) : TicketingServer {

  override fun analyzeContracts(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: AnalyzeContractsInputDto
  ): AnalyzeContractsOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.READ_CARD_AND_ANALYZE_CONTRACTS.name,
        localReaderName,
        smartCard,
        inputData,
        AnalyzeContractsOutputDto::class.java)
  }

  override fun personalizeCard(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: PersonalizeCardInputDto
  ): PersonalizeCardOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.PERSONALIZE_CARD.name,
        localReaderName,
        smartCard,
        inputData,
        PersonalizeCardOutputDto::class.java)
  }

  override fun loadContract(
      localReaderName: String,
      smartCard: SmartCard,
      inputData: LoadContractInputDto
  ): LoadContractOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.READ_CARD_AND_LOAD_CONTRACT.name,
        localReaderName,
        smartCard,
        inputData,
        LoadContractOutputDto::class.java)
  }
}
