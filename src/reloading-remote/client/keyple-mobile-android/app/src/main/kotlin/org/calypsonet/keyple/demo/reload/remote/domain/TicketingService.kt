/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.domain

import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject
import kotlin.jvm.Throws
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceOutputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractOutputDto
import org.calypsonet.keyple.demo.reload.remote.di.scopes.AppScoped
import org.calypsonet.keyple.demo.reload.remote.domain.model.CardProtocolEnum
import org.calypsonet.keyple.demo.reload.remote.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_CLASSIC_1K
import org.eclipse.keypop.storagecard.card.ProductType.MIFARE_ULTRALIGHT
import org.eclipse.keypop.storagecard.card.ProductType.ST25_SRT512

@AppScoped
class TicketingService
@Inject
constructor(
    private var keypopApiProvider: KeypopApiProvider,
    private var readerManager: ReaderManager,
    private var logger: Logger,
    private var remoteServiceManager: RemoteServiceManager
) {

  /** Select the card and retrieve the active card */
  @Throws(IllegalStateException::class, Exception::class)
  fun getSmartCard(readerName: String, aidEnums: ArrayList<ByteArray>): SmartCard {
    with(readerManager.getReader(readerName)) {
      val readerApiFactory = keypopApiProvider.getReaderApiFactory()

      val reader = readerManager.getReader(readerName)

      val storageCardApiFactory = keypopApiProvider.getStorageCardApiFactory()

      val cardSelectionManager = readerApiFactory.createCardSelectionManager()

      aidEnums.forEach {
        /**
         * Generic selection: configures a CardSelector with all the desired attributes to perform
         * the selection and read additional information afterward
         */
        val calypsoCardSelector =
            readerApiFactory
                .createIsoCardSelector()
                .filterByCardProtocol(CardProtocolEnum.ISO_14443_4_LOGICAL_PROTOCOL.name)
                .filterByDfName(it)
        cardSelectionManager.prepareSelection(
            calypsoCardSelector,
            keypopApiProvider.getCalypsoCardApiFactory().createCalypsoCardSelectionExtension())
      }

      try {
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createBasicCardSelector()
                .filterByCardProtocol(CardProtocolEnum.MIFARE_ULTRALIGHT_LOGICAL_PROTOCOL.name),
            storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_ULTRALIGHT))
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createBasicCardSelector()
                .filterByCardProtocol(CardProtocolEnum.ST25_SRT512_LOGICAL_PROTOCOL.name),
            storageCardApiFactory.createStorageCardSelectionExtension(ST25_SRT512))
        cardSelectionManager.prepareSelection(
            readerApiFactory
                .createBasicCardSelector()
                .filterByCardProtocol(CardProtocolEnum.MIFARE_CLASSIC_LOGICAL_PROTOCOL.name),
            storageCardApiFactory.createStorageCardSelectionExtension(MIFARE_CLASSIC_1K))
      } catch (e: Exception) {
        logger.e("$e")
      }

      val selectionResult = cardSelectionManager.processCardSelectionScenario(reader)
      val smartCard = selectionResult.activeSmartCard
      if (smartCard != null) {
          // TODO move this code to the calling method
          //          val calypsoCard = selectionResult.activeSmartCard as CalypsoCard
          //          // check is the DF name is the expected one (Req. TL-SEL-AIDMATCH.1)
          //          if (!CardConstants.aidMatch(
          //              aidEnums[selectionResult.activeSelectionIndex], calypsoCard.dfName)) {
          //            throw IllegalStateException("Unexpected DF name")
          //          }
          return smartCard
      } else {
        throw IllegalStateException("Matching smartcard not found")
      }
    }
  }

  fun analyzeContracts(
      localReaderName: String,
      smartCard: SmartCard,
      input: AnalyzeContractsInputDto
  ): AnalyzeContractsOutputDto {
    return remoteServiceManager.analyzeContracts(localReaderName, smartCard, input)
  }

  fun personalizeCard(
      localReaderName: String,
      smartCard: SmartCard,
      input: CardIssuanceInputDto
  ): CardIssuanceOutputDto {
    return remoteServiceManager.personalizeCard(localReaderName, smartCard, input)
  }

  fun writeContract(
      localReaderName: String,
      smartCard: SmartCard,
      input: WriteContractInputDto
  ): WriteContractOutputDto {
    return remoteServiceManager.writeContract(localReaderName, smartCard, input)
  }
}
