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
package org.calypsonet.keyple.demo.sales.domain.usecases

import org.calypsonet.keyple.demo.common.dto.PersonalizeCardInputDto
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus
import org.calypsonet.keyple.demo.sales.domain.CardReaderSession
import org.calypsonet.keyple.demo.sales.domain.mappers.toCardInfo
import org.calypsonet.keyple.demo.sales.domain.model.CardOperationResult
import org.calypsonet.keyple.demo.sales.domain.spi.Logger
import org.calypsonet.keyple.demo.sales.domain.spi.TicketingServer

/** Personalization of the presented card, by the server. */
class PersonalizeCard(
    private val cardReaderSession: CardReaderSession,
    private val ticketingServer: TicketingServer,
    private val logger: Logger
) {

  /**
   * Selects the presented card and asks the server to personalize it.
   *
   * @throws IllegalStateException If no supported card is selected.
   */
  operator fun invoke(): CardOperationResult {
    val smartCard = cardReaderSession.selectCard()
    val output =
        ticketingServer.personalizeCard(
            cardReaderSession.readerName,
            smartCard,
            PersonalizeCardInputDto(cardReaderSession.pluginType))
    val result =
        CardOperationResult(smartCard.toCardInfo(), RemoteServiceStatus.fromCode(output.statusCode))
    if (result.status == RemoteServiceStatus.SUCCESS) {
      logger.i("Card personalized [serialNumber=${result.card.serialNumber}]")
    } else {
      logger.w(
          "Personalization failed [serialNumber=${result.card.serialNumber}, status=${result.status}]")
    }
    return result
  }
}
