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
package org.calypsonet.keyple.demo.reload.remote.domain.usecases

import java.time.LocalDate
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toCardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toContract
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReadContractsResult
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager

/** Reading of the contracts of the presented card, analyzed by the server. */
class ReadContracts(
    private val cardReaderSession: CardReaderSession,
    private val remoteServiceManager: RemoteServiceManager
) {

  /**
   * Selects the presented card and asks the server to read and analyze its contracts.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  operator fun invoke(): ReadContractsResult {
    val smartCard = cardReaderSession.selectCard()
    val output =
        remoteServiceManager.analyzeContracts(
            cardReaderSession.readerName,
            smartCard,
            AnalyzeContractsInputDto(cardReaderSession.pluginType))
    val status = RemoteServiceStatus.fromCode(output.statusCode)
    val today = LocalDate.now()
    val contracts =
        if (status == RemoteServiceStatus.SUCCESS) output.contracts.map { it.toContract(today) }
        else emptyList()
    return ReadContractsResult(smartCard.toCardInfo(), status, contracts)
  }
}
