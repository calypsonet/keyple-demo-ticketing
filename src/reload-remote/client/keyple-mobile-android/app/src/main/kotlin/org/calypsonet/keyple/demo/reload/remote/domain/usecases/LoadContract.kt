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
import org.calypsonet.keyple.demo.common.dto.LoadContractInputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toCardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardOperationResult
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.TicketingServer

/** Loading by the server of the contract bought in the presented card. */
class LoadContract(
    private val cardReaderSession: CardReaderSession,
    private val ticketingServer: TicketingServer,
    private val logger: Logger
) {

  /**
   * Selects the presented card and asks the server to load the given contract.
   *
   * @param expectedSerialNumber Serial number of the card for which the contract has been bought.
   * @param contractTariff The contract to load.
   * @param tripsToLoad The number of trips to load (multi-trip contract).
   * @return The result of the operation, with the status [RemoteServiceStatus.DIFFERENT_CARD] if
   *   the presented card is not the one for which the contract has been bought.
   * @throws IllegalStateException If no supported card is selected.
   */
  operator fun invoke(
      expectedSerialNumber: String?,
      contractTariff: PriorityCode,
      tripsToLoad: Int
  ): CardOperationResult {
    val smartCard = cardReaderSession.selectCard()
    val card = smartCard.toCardInfo()
    if (card.serialNumber != expectedSerialNumber) {
      // The contract has been bought for the card read at the first step: the loading must be done
      // on the same card.
      return CardOperationResult(card, RemoteServiceStatus.DIFFERENT_CARD).also {
        log(it, contractTariff, tripsToLoad)
      }
    }
    val readerName = cardReaderSession.readerName
    val pluginType = cardReaderSession.pluginType
    ticketingServer.analyzeContracts(readerName, smartCard, AnalyzeContractsInputDto(pluginType))
    val output =
        ticketingServer.loadContract(
            readerName, smartCard, LoadContractInputDto(contractTariff, tripsToLoad, pluginType))
    return CardOperationResult(card, RemoteServiceStatus.fromCode(output.statusCode)).also {
      log(it, contractTariff, tripsToLoad)
    }
  }

  /** Logs the result of the loading. */
  private fun log(result: CardOperationResult, contractTariff: PriorityCode, tripsToLoad: Int) {
    if (result.status == RemoteServiceStatus.SUCCESS) {
      logger.i(
          "Contract loaded [serialNumber=${result.card.serialNumber}, " +
              "contractTariff=$contractTariff, tripsToLoad=$tripsToLoad]")
    } else {
      logger.w(
          "Contract loading failed [serialNumber=${result.card.serialNumber}, status=${result.status}]")
    }
  }
}
