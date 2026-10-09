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
package org.calypsonet.keyple.demo.reload.remote.data

import javax.inject.Inject
import org.calypsonet.keyple.demo.common.constants.RemoteServiceId
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceOutputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractOutputDto
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager
import org.eclipse.keyple.distributed.LocalServiceClient
import org.eclipse.keypop.reader.selection.spi.SmartCard

class RemoteServiceManagerImpl
@Inject
constructor(private val localServiceClient: LocalServiceClient) : RemoteServiceManager {

  override fun analyzeContracts(
      localReaderName: String,
      smartCard: SmartCard,
      input: AnalyzeContractsInputDto
  ): AnalyzeContractsOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.READ_CARD_AND_ANALYZE_CONTRACTS.name,
        localReaderName,
        smartCard,
        input,
        AnalyzeContractsOutputDto::class.java)
  }

  override fun personalizeCard(
      localReaderName: String,
      smartCard: SmartCard,
      input: CardIssuanceInputDto
  ): CardIssuanceOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.PERSONALIZE_CARD.name,
        localReaderName,
        smartCard,
        input,
        CardIssuanceOutputDto::class.java)
  }

  override fun writeContract(
      localReaderName: String,
      smartCard: SmartCard,
      input: WriteContractInputDto
  ): WriteContractOutputDto {
    return localServiceClient.executeRemoteService(
        RemoteServiceId.READ_CARD_AND_WRITE_CONTRACT.name,
        localReaderName,
        smartCard,
        input,
        WriteContractOutputDto::class.java)
  }
}
