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
package org.calypsonet.keyple.demo.validation.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.parser.SCContractStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEnvironmentHolderStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEventStructureParser
import org.calypsonet.keyple.demo.validation.domain.Messages
import org.calypsonet.keyple.demo.validation.domain.ValidationException
import org.calypsonet.keyple.demo.validation.domain.ValidationRules
import org.calypsonet.keyple.demo.validation.domain.mapper.ValidationMapper
import org.calypsonet.keyple.demo.validation.domain.model.AppSettings
import org.calypsonet.keyple.demo.validation.domain.model.CardReaderResponse
import org.calypsonet.keyple.demo.validation.domain.model.Location
import org.calypsonet.keyple.demo.validation.domain.model.Status
import org.calypsonet.keyple.demo.validation.domain.model.Validation
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.storagecard.card.StorageCard
import org.eclipse.keypop.storagecard.transaction.ChannelControl
import timber.log.Timber

class StorageCardRepository {

  fun executeValidationProcedure(
      validationDateTime: LocalDateTime,
      validationAmount: Int,
      cardReader: CardReader,
      storageCard: StorageCard,
      locations: List<Location>
  ): CardReaderResponse {
    var status: Status = Status.LOADING
    var errorMessage: String? = null
    var passValidityEndDate: LocalDate? = null
    var nbTicketsLeft: Int? = null
    var validation: Validation? = null

    val storageCardExtension = StorageCardExtensionService.getInstance()

    // Create a card transaction for validation
    val cardTransaction =
        try {
          storageCardExtension.createStorageCardTransactionManager(cardReader, storageCard)
        } catch (e: Exception) {
          Timber.w(e)
          status = Status.ERROR
          errorMessage = e.message
          null
        }

    if (cardTransaction != null) {
      try {
        // ***************** Event and Environment Analysis
        // Step 1 - Read the environment and event data
        cardTransaction
            .prepareReadBlocks(
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
            .prepareReadBlocks(CardConstant.SC_EVENT_FIRST_BLOCK, CardConstant.SC_EVENT_LAST_BLOCK)
            .prepareReadBlocks(
                CardConstant.SC_CONTRACT_FIRST_BLOCK, CardConstant.SC_COUNTER_LAST_BLOCK)
            .processCommands(ChannelControl.KEEP_OPEN)

        // Step 2 - Unpack environment structure
        val environmentContent =
            storageCard.getBlocks(
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
        val environment = SCEnvironmentHolderStructureParser().parse(environmentContent)

        // Step 3 - Validate environment version
        ValidationRules.validateEnvironmentVersionOrThrow(environment.envVersionNumber)

        // Step 4 - Validate environment end date
        ValidationRules.validateEnvironmentDateOrThrow(
            environment.envEndDate.getDate(), validationDateTime.toLocalDate())

        // Step 5 - Read and unpack the event record
        val eventContent =
            storageCard.getBlocks(
                CardConstant.SC_EVENT_FIRST_BLOCK, CardConstant.SC_EVENT_LAST_BLOCK)
        val event = SCEventStructureParser().parse(eventContent)

        // Step 6 - Validate event version
        ValidationRules.validateEventVersionOrThrow(event.eventVersionNumber)

        // Step 7 - Read and unpack the contract record
        val contractContent =
            storageCard.getBlocks(
                CardConstant.SC_CONTRACT_FIRST_BLOCK, CardConstant.SC_COUNTER_LAST_BLOCK)
        val contract = SCContractStructureParser().parse(contractContent)

        // Validate contract version
        ValidationRules.validateContractVersionOrThrow(contract.contractVersionNumber)

        // Check contract validity
        try {
          ValidationRules.validateContractDateOrThrow(
              contract.contractValidityEndDate.getDate(), validationDateTime.toLocalDate())
        } catch (e: ValidationException) {
          status = e.status
          errorMessage = e.message
          throw e
        }

        // Determine contract priority from contract tariff
        val contractPriority = contract.contractTariff

        var writeEvent: Boolean
        val contractUsed = 1 // For storage card, we only have one contract

        when (contractPriority) {
          PriorityCode.MULTI_TRIP -> {
            // Check if there are trips left
            val counterValue = contract.counterValue ?: 0
            ValidationRules.validateTripsAvailableOrThrow(counterValue)

            // Decrement counter
            val newCounterValue =
                counterValue -
                    ValidationRules.calculateDecrementAmount(contractPriority, validationAmount)
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(CardConstant.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
                .processCommands(ChannelControl.KEEP_OPEN)

            writeEvent = true
          }
          PriorityCode.STORED_VALUE -> {
            // Check if there's enough value
            val counterValue = contract.counterValue ?: 0
            ValidationRules.validateSufficientStoredValueOrThrow(counterValue, validationAmount)

            // Decrement counter by validation amount
            val newCounterValue = counterValue - validationAmount
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(CardConstant.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
                .processCommands(ChannelControl.KEEP_OPEN)

            writeEvent = true
          }
          PriorityCode.SEASON_PASS -> {
            passValidityEndDate = contract.contractValidityEndDate.getDate()
            writeEvent = true
          }
          PriorityCode.FORBIDDEN,
          PriorityCode.EXPIRED,
          PriorityCode.UNKNOWN -> {
            throw ValidationException(
                Messages.EXCEPTION_CONTRACT_FORBIDDEN_OR_EXPIRED, Status.EMPTY_CARD)
          }
        }

        if (writeEvent) {
          // Create a new validation event
          val eventToWrite =
              EventStructure(
                  eventVersionNumber = VersionNumber.CURRENT_VERSION,
                  eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
                  eventTimeStamp = TimeCompact(validationDateTime),
                  eventLocation = AppSettings.location.id,
                  eventContractUsed = contractUsed,
                  contractPriority1 = contractPriority,
                  contractPriority2 = PriorityCode.FORBIDDEN,
                  contractPriority3 = PriorityCode.FORBIDDEN,
                  contractPriority4 = PriorityCode.FORBIDDEN)

          validation = ValidationMapper.map(eventToWrite, locations)

          // Write the event
          val eventBytesToWrite = SCEventStructureParser().generate(eventToWrite)
          cardTransaction
              .prepareWriteBlocks(CardConstant.SC_EVENT_FIRST_BLOCK, eventBytesToWrite)
              .processCommands(ChannelControl.KEEP_OPEN)

          Timber.i(Messages.LOG_VALIDATION_SUCCESS)
          status = Status.SUCCESS
          errorMessage = null
        } else {
          Timber.i(Messages.LOG_VALIDATION_FAILED_NO_CONTRACT)
          errorMessage = Messages.ERROR_NO_VALID_TITLE_DETECTED
        }
      } catch (e: ValidationException) {
        Timber.e(e)
        status = e.status
        errorMessage = e.message
        if (status == Status.LOADING) {
          status = Status.ERROR
        }
      } catch (e: Exception) {
        Timber.e(e)
        status = Status.ERROR
        errorMessage = e.message
      } finally {
        // Close the transaction
        try {
          cardTransaction.processCommands(ChannelControl.CLOSE_AFTER)
        } catch (e: Exception) {
          Timber.e(e)
          if (status == Status.LOADING) {
            status = Status.ERROR
          }
          if (errorMessage.isNullOrEmpty()) {
            errorMessage = e.message
          }
        }
      }
    }

    return CardReaderResponse(
        status = status,
        cardType = storageCard.productType.name,
        nbTicketsLeft = nbTicketsLeft,
        contract = Messages.EMPTY_CONTRACT,
        validation = validation,
        errorMessage = errorMessage,
        passValidityEndDate = passValidityEndDate,
        eventDateTime = validationDateTime)
  }
}
