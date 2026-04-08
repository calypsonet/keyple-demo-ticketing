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
package org.calypsonet.keyple.demo.reload.remote.domain.spi

import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceOutputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractOutputDto
import org.eclipse.keypop.reader.selection.spi.SmartCard

interface RemoteServiceManager {
  fun analyzeContracts(
      localReaderName: String,
      smartCard: SmartCard,
      input: AnalyzeContractsInputDto
  ): AnalyzeContractsOutputDto

  fun personalizeCard(
      localReaderName: String,
      smartCard: SmartCard,
      input: CardIssuanceInputDto
  ): CardIssuanceOutputDto

  fun writeContract(
      localReaderName: String,
      smartCard: SmartCard,
      input: WriteContractInputDto
  ): WriteContractOutputDto
}
