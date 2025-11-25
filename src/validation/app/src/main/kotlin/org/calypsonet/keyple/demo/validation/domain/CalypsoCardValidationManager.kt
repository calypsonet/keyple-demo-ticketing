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
import org.calypsonet.keyple.demo.common.parser.ContractStructureParser
import org.calypsonet.keyple.demo.common.parser.EnvironmentHolderStructureParser
import org.calypsonet.keyple.demo.common.parser.EventStructureParser
import org.calypsonet.keyple.demo.validation.domain.builders.ValidationDataBuilder
import org.calypsonet.keyple.demo.validation.domain.model.AppSettings
import org.calypsonet.keyple.demo.validation.domain.model.Status
import org.calypsonet.keyple.demo.validation.domain.model.ValidationData
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.ChannelControl
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.reader.CardReader

class CalypsoCardValidationManager : BaseValidationManager() {

  fun executeValidationProcedure(
      validationDateTime: LocalDateTime,
      validationAmount: Int,
      cardReader: CardReader,
      calypsoCard: CalypsoCard,
      cardSecuritySettings: SymmetricCryptoSecuritySetting,
      locations: List<Location>,
      keypopApiProvider: KeypopApiProvider
  ): ValidationResult {

    var status: Status = Status.LOADING
    var errorMessage: String? = null
    val cardTransaction: SecureRegularModeTransactionManager?
    var passValidityEndDate: LocalDate? = null
    var nbTicketsLeft: Int? = null
    var validationData: ValidationData? = null

    val calypsoCardApiFactory = keypopApiProvider.getCalypsoCardApiFactory()

    // Create a card transaction for validation.
    cardTransaction =
        try {
          calypsoCardApiFactory.createSecureRegularModeTransactionManager(
              cardReader, calypsoCard, cardSecuritySettings)
        } catch (e: Exception) {
          status = Status.ERROR
          errorMessage = e.message
          null
        }

    if (cardTransaction != null) {
      try {

        // ***************** Event and Environment Analysis
        // Step 1 - Open a Validation session reading the environment record.
        cardTransaction
            .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
            .prepareReadRecords(
                CardConstant.Companion.SFI_ENVIRONMENT_AND_HOLDER,
                1,
                1,
                CardConstant.Companion.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
            .processCommands(ChannelControl.KEEP_OPEN)

        // Step 2 - Unpack environment structure from the binary present in the environment record.
        val efEnvironmentHolder =
            calypsoCard.getFileBySfi(CardConstant.Companion.SFI_ENVIRONMENT_AND_HOLDER)
        val environmentContent = efEnvironmentHolder.data.content
        val environment = EnvironmentHolderStructureParser().parse(environmentContent)

        // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1
        // for the current version) reject the card. <Abort Secure Session>
        validateEnvironmentVersionOrThrow(environment.envVersionNumber)

        // Step 4 - If EnvEndDate points to a date in the past reject the card. <Abort Secure
        // Session>
        validateEnvironmentDateOrThrow(
            environment.envEndDate.getDate(), validationDateTime.toLocalDate())

        // Step 5 - Read and unpack the last event record.
        cardTransaction
            .prepareReadRecords(
                CardConstant.Companion.SFI_EVENTS_LOG,
                1,
                1,
                CardConstant.Companion.EVENT_RECORD_SIZE_BYTES)
            .processCommands(ChannelControl.KEEP_OPEN)

        val efEventLog = calypsoCard.getFileBySfi(CardConstant.Companion.SFI_EVENTS_LOG)
        val eventContent = efEventLog.data.content
        val event = EventStructureParser().parse(eventContent)

        // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version)
        // reject the card. <Abort Secure Session>
        validateEventVersionOrThrow(event.eventVersionNumber)

        // Step 6.2 - anti-passback management & communication failure recovery
        validateAntiPassbackOrThrow(
            event.eventDatetime, validationDateTime, calypsoCard.isDfRatified)

        // ***************** Best Contract Search
        // Step 7 - Create a list of PriorityCode fields that are different from FORBIDDEN and
        // EXPIRED.
        val allPriorities =
            listOf(
                Pair(1, event.contractPriority1),
                Pair(2, event.contractPriority2),
                Pair(3, event.contractPriority3),
                Pair(4, event.contractPriority4))

        // Step 9 - If the list is empty go to END.
        validateHasValidContractsOrThrow(allPriorities)
        val filteredPriorities = filterValidContractPriorities(allPriorities)

        var priority1 = event.contractPriority1
        var priority2 = event.contractPriority2
        var priority3 = event.contractPriority3
        var priority4 = event.contractPriority4
        var contractUsed = 0
        var writeEvent = false

        // Step 10 - For each element in the list:
        val sortedPriorities = sortContractPrioritiesByPriority(filteredPriorities)

        // Step 11 - For each element in the list:
        for (it in sortedPriorities) {
          val record = it.first
          val contractPriority = it.second

          // Step 11.1 - Read and unpack the contract record for the index being iterated.
          cardTransaction
              .prepareReadRecords(
                  CardConstant.Companion.SFI_CONTRACTS,
                  record,
                  record,
                  CardConstant.Companion.CONTRACT_RECORD_SIZE_BYTES)
              .processCommands(ChannelControl.KEEP_OPEN)

          val efContractParser = calypsoCard.getFileBySfi(CardConstant.Companion.SFI_CONTRACTS)
          val contractContent = efContractParser.data.allRecordsContent[record]!!
          val contract = ContractStructureParser().parse(contractContent)

          // Step 11.2 - If ContractVersionNumber is not the expected one (==1 for the current
          // version) reject the card. <Abort Secure Session>
          validateContractVersionOrThrow(contract.contractVersionNumber)

          // Step 11.3 - '  If ContractAuthenticator is not 0 perform the verification of the value
          // by using the PSO Verify Signature command of the SAM.
          @Suppress("ControlFlowWithEmptyBody")
          if (contract.contractAuthenticator != 0) {
            // Step 11.3.1 - If the value is wrong reject the card. <Abort Secure Session>
            // Step 11.3.2 - If the value of ContractSaleSam is present in the SAM Black List reject
            // the card. <Abort Secure Session>
            // TODO: steps 11.3.1 & 11.3.2
          }

          // Step 11.4 - If ContractValidityEndDate points to a date in the past update the
          // associated ContractPriorty field present in the persistent object to 31 and move to the
          // next element in the list
          try {
            validateContractDateOrThrow(
                contract.contractValidityEndDate.getDate(), validationDateTime.toLocalDate())
          } catch (e: ValidationException) {
            when (record) {
              1 -> priority1 = PriorityCode.EXPIRED
              2 -> priority2 = PriorityCode.EXPIRED
              3 -> priority3 = PriorityCode.EXPIRED
              4 -> priority4 = PriorityCode.EXPIRED
            }
            status = e.status
            errorMessage = e.message
            writeEvent = true
            continue
          }

          // Step 11.5 - If the ContractTariff value for the contract read is 2 or 3:
          if (isCounterBasedContract(contractPriority)) {

            val nbContractRecords =
                when (calypsoCard.productType) {
                  CalypsoCard.ProductType.BASIC -> 1
                  CalypsoCard.ProductType.LIGHT -> 2
                  else -> 4
                }

            // Step 11.5.1 - Read and unpack the counter associated to the contract (1st counter for
            // Contract #1 and so forth).
            cardTransaction
                .prepareReadCounter(CardConstant.Companion.SFI_COUNTERS, nbContractRecords)
                .processCommands(ChannelControl.KEEP_OPEN)

            val efCounter = calypsoCard.getFileBySfi(CardConstant.Companion.SFI_COUNTERS)
            val counterValue = efCounter.data.getContentAsCounterValue(record)

            // Step 11.5.2 - If the counter value is 0 update the associated ContractPriorty field
            // present in the persistent object to 31 and move to the next element in the list
            try {
              validateTripsAvailableOrThrow(counterValue)
            } catch (e: ValidationException) {
              when (record) {
                1 -> priority1 = PriorityCode.EXPIRED
                2 -> priority2 = PriorityCode.EXPIRED
                3 -> priority3 = PriorityCode.EXPIRED
                4 -> priority4 = PriorityCode.EXPIRED
              }
              status = e.status
              errorMessage = e.message
              writeEvent = true
              continue
            }

            // Step 11.5.3 - If the counter value is > 0 && ContractTariff == 3 && CounterValue <
            // ValidationAmount move to the next element in the list
            if (contractPriority == PriorityCode.STORED_VALUE) {
              try {
                validateSufficientStoredValueOrThrow(counterValue, validationAmount)
              } catch (e: ValidationException) {
                status = e.status
                errorMessage = e.message
                continue
              }
            }
            // Step 11.5.4 - UPDATE COUNTER Decrement the counter value by the appropriate amount (1
            // if ContractTariff is 2, and the configured value for the trip if ContractTariff is
            // 3).
            else {
              val decrement = calculateDecrementAmount(contractPriority, validationAmount)
              if (decrement > 0) {
                cardTransaction
                    .prepareDecreaseCounter(CardConstant.Companion.SFI_COUNTERS, record, decrement)
                    .processCommands(ChannelControl.KEEP_OPEN)
                nbTicketsLeft = counterValue - decrement
              }
            }
          } else if (contractPriority == PriorityCode.SEASON_PASS) {
            passValidityEndDate = contract.contractValidityEndDate.getDate()
          }

          // We will create a new event for this contract
          contractUsed = record
          writeEvent = true
          break
        }

        if (writeEvent) {

          val eventToWrite: EventStructure
          if (contractUsed > 0) {
            // Create a new validation event
            eventToWrite =
                EventStructure(
                    eventVersionNumber = VersionNumber.CURRENT_VERSION,
                    eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
                    eventTimeStamp = TimeCompact(validationDateTime),
                    eventLocation = AppSettings.location.id,
                    eventContractUsed = contractUsed,
                    contractPriority1 = priority1,
                    contractPriority2 = priority2,
                    contractPriority3 = priority3,
                    contractPriority4 = priority4)
            validationData = ValidationDataBuilder.buildFrom(eventToWrite, locations)

            status = Status.SUCCESS
            errorMessage = null
          } else {
            // Update old event's priorities
            eventToWrite =
                EventStructure(
                    eventVersionNumber = event.eventVersionNumber,
                    eventDateStamp = event.eventDateStamp,
                    eventTimeStamp = event.eventTimeStamp,
                    eventLocation = event.eventLocation,
                    eventContractUsed = event.eventContractUsed,
                    contractPriority1 = priority1,
                    contractPriority2 = priority2,
                    contractPriority3 = priority3,
                    contractPriority4 = priority4)
          }

          // Step 13 - Pack the Event structure and append it to the event file
          val eventBytesToWrite = EventStructureParser().generate(eventToWrite)
          cardTransaction
              .prepareUpdateRecord(CardConstant.Companion.SFI_EVENTS_LOG, 1, eventBytesToWrite)
              .processCommands(ChannelControl.KEEP_OPEN)
        } else {
          if (errorMessage.isNullOrEmpty()) {
            errorMessage = ERROR_NO_VALID_TITLE_DETECTED
          }
        }
      } catch (e: ValidationException) {
        status = e.status
        errorMessage = e.message
      } catch (e: Exception) {
        status = Status.ERROR
        errorMessage = e.message
      } finally {
        // Step 14 - END: Close the session
        try {
          if (status == Status.SUCCESS) {
            cardTransaction.prepareCloseSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
          } else {
            cardTransaction.prepareCancelSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
          }
          if (status == Status.LOADING) {
            status = Status.ERROR
          }
        } catch (e: Exception) {
          errorMessage = e.message
          status = Status.ERROR
        }
      }
    }

    return ValidationResult(
        status = status,
        cardType = CARD_TYPE_CALYPSO_PREFIX + HexUtil.toHex(calypsoCard.dfName),
        nbTicketsLeft = nbTicketsLeft,
        contract = EMPTY_CONTRACT,
        validationData = validationData,
        errorMessage = errorMessage,
        passValidityEndDate = passValidityEndDate,
        eventDateTime = validationDateTime)
  }
}
