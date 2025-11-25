/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.constant.CardConstant
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.common.parser.SCContractStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEnvironmentHolderStructureParser
import org.calypsonet.keyple.demo.common.parser.SCEventStructureParser
import org.calypsonet.keyple.demo.validation.domain.mappers.ValidationDataMapper
import org.calypsonet.keyple.demo.validation.domain.model.AppSettings
import org.calypsonet.keyple.demo.validation.domain.model.Status
import org.calypsonet.keyple.demo.validation.domain.model.ValidationData
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.storagecard.card.StorageCard
import org.eclipse.keypop.storagecard.transaction.ChannelControl

class StorageCardValidationManager : BaseValidationManager() {

  fun executeValidationProcedure(
      validationDateTime: LocalDateTime,
      validationAmount: Int,
      cardReader: CardReader,
      storageCard: StorageCard,
      locations: List<Location>,
      keypopApiProvider: KeypopApiProvider
  ): ValidationResult {
    var status: Status = Status.LOADING
    var errorMessage: String? = null
    var passValidityEndDate: LocalDate? = null
    var nbTicketsLeft: Int? = null
    var validationData: ValidationData? = null

    val storageCardApiFactory = keypopApiProvider.getStorageCardApiFactory()

    // Create a card transaction for validation
    val cardTransaction =
        try {
          storageCardApiFactory.createStorageCardTransactionManager(cardReader, storageCard)
        } catch (e: Exception) {
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
                CardConstant.Companion.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.Companion.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
            .prepareReadBlocks(
                CardConstant.Companion.SC_EVENT_FIRST_BLOCK,
                CardConstant.Companion.SC_EVENT_LAST_BLOCK)
            .prepareReadBlocks(
                CardConstant.Companion.SC_CONTRACT_FIRST_BLOCK,
                CardConstant.Companion.SC_COUNTER_LAST_BLOCK)
            .processCommands(ChannelControl.KEEP_OPEN)

        // Step 2 - Unpack environment structure
        val environmentContent =
            storageCard.getBlocks(
                CardConstant.Companion.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                CardConstant.Companion.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
        val environment = SCEnvironmentHolderStructureParser().parse(environmentContent)

        // Step 3 - Validate environment version
        validateEnvironmentVersionOrThrow(environment.envVersionNumber)

        // Step 4 - Validate environment end date
        validateEnvironmentDateOrThrow(
            environment.envEndDate.getDate(), validationDateTime.toLocalDate())

        // Step 5 - Read and unpack the event record
        val eventContent =
            storageCard.getBlocks(
                CardConstant.Companion.SC_EVENT_FIRST_BLOCK,
                CardConstant.Companion.SC_EVENT_LAST_BLOCK)
        val event = SCEventStructureParser().parse(eventContent)

        // Step 6 - Validate event version
        validateEventVersionOrThrow(event.eventVersionNumber)

        // Step 7 - Read and unpack the contract record
        val contractContent =
            storageCard.getBlocks(
                CardConstant.Companion.SC_CONTRACT_FIRST_BLOCK,
                CardConstant.Companion.SC_COUNTER_LAST_BLOCK)
        val contract = SCContractStructureParser().parse(contractContent)

        // Validate contract version
        validateContractVersionOrThrow(contract.contractVersionNumber)

        // Check contract validity
        try {
          validateContractDateOrThrow(
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
            validateTripsAvailableOrThrow(counterValue)

            // Decrement counter
            val newCounterValue =
                counterValue - calculateDecrementAmount(contractPriority, validationAmount)
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(
                    CardConstant.Companion.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
                .processCommands(ChannelControl.KEEP_OPEN)

            writeEvent = true
          }
          PriorityCode.STORED_VALUE -> {
            // Check if there's enough value
            val counterValue = contract.counterValue ?: 0
            validateSufficientStoredValueOrThrow(counterValue, validationAmount)

            // Decrement counter by validation amount
            val newCounterValue = counterValue - validationAmount
            contract.counterValue = newCounterValue
            nbTicketsLeft = newCounterValue

            // Update contract data
            val updatedContractContent = SCContractStructureParser().generate(contract)
            cardTransaction
                .prepareWriteBlocks(
                    CardConstant.Companion.SC_CONTRACT_FIRST_BLOCK, updatedContractContent)
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
            throw ValidationException(EXCEPTION_CONTRACT_FORBIDDEN_OR_EXPIRED, Status.EMPTY_CARD)
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

          validationData = ValidationDataMapper.map(eventToWrite, locations)

          // Write the event
          val eventBytesToWrite = SCEventStructureParser().generate(eventToWrite)
          cardTransaction
              .prepareWriteBlocks(CardConstant.Companion.SC_EVENT_FIRST_BLOCK, eventBytesToWrite)
              .processCommands(ChannelControl.KEEP_OPEN)

          status = Status.SUCCESS
          errorMessage = null
        } else {
          errorMessage = ERROR_NO_VALID_TITLE_DETECTED
        }
      } catch (e: ValidationException) {
        status = e.status
        errorMessage = e.message
        if (status == Status.LOADING) {
          status = Status.ERROR
        }
      } catch (e: Exception) {
        status = Status.ERROR
        errorMessage = e.message
      } finally {
        // Close the transaction
        try {
          cardTransaction.processCommands(ChannelControl.CLOSE_AFTER)
        } catch (e: Exception) {
          if (status == Status.LOADING) {
            status = Status.ERROR
          }
          if (errorMessage.isNullOrEmpty()) {
            errorMessage = e.message
          }
        }
      }
    }

    return ValidationResult(
        status = status,
        cardType = storageCard.productType.name,
        nbTicketsLeft = nbTicketsLeft,
        contract = EMPTY_CONTRACT,
        validationData = validationData,
        errorMessage = errorMessage,
        passValidityEndDate = passValidityEndDate,
        eventDateTime = validationDateTime)
  }
}
