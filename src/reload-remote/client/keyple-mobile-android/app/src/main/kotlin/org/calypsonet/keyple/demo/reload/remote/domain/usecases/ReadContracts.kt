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

import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toCardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toContract
import org.calypsonet.keyple.demo.reload.remote.domain.model.ReadContractsResult
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Clock
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.TicketingServer

/** Reading of the contracts of the presented card, analyzed by the server. */
class ReadContracts(
    private val cardReaderSession: CardReaderSession,
    private val ticketingServer: TicketingServer,
    private val clock: Clock,
    private val logger: Logger
) {

  /**
   * Selects the presented card and asks the server to read and analyze its contracts.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  operator fun invoke(): ReadContractsResult {
    val smartCard = cardReaderSession.selectCard()
    val output =
        ticketingServer.analyzeContracts(
            cardReaderSession.readerName,
            smartCard,
            AnalyzeContractsInputDto(cardReaderSession.pluginType))
    val status = RemoteServiceStatus.fromCode(output.statusCode)
    val today = clock.now().toLocalDate()
    val contracts =
        if (status == RemoteServiceStatus.SUCCESS) output.contracts.map { it.toContract(today) }
        else emptyList()
    val card = smartCard.toCardInfo()
    if (status == RemoteServiceStatus.SUCCESS) {
      logger.i("Contracts read [serialNumber=${card.serialNumber}, contracts=${contracts.size}]")
    } else {
      logger.w("Contracts reading failed [serialNumber=${card.serialNumber}, status=$status]")
    }
    return ReadContractsResult(card, status, contracts)
  }
}
