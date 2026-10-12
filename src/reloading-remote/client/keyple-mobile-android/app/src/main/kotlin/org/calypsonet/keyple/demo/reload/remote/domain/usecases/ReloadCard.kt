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
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.mappers.toCardInfo
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardOperationResult
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager

/** Reload of the presented card with the contract bought, by the server. */
class ReloadCard(
    private val cardReaderSession: CardReaderSession,
    private val remoteServiceManager: RemoteServiceManager
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
      // The contract has been bought for the card read at the first step: the reload must be done
      // on the same card.
      return CardOperationResult(card, RemoteServiceStatus.DIFFERENT_CARD)
    }
    val readerName = cardReaderSession.readerName
    val pluginType = cardReaderSession.pluginType
    remoteServiceManager.analyzeContracts(
        readerName, smartCard, AnalyzeContractsInputDto(pluginType))
    val output =
        remoteServiceManager.writeContract(
            readerName, smartCard, WriteContractInputDto(contractTariff, tripsToLoad, pluginType))
    return CardOperationResult(card, RemoteServiceStatus.fromCode(output.statusCode))
  }
}
