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
package org.calypsonet.keyple.demo.validation.domain.procedures

import java.time.LocalDate
import org.calypsonet.keyple.demo.common.codecs.CalypsoContractCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEventCodec
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.domain.builders.ValidationDataBuilder
import org.calypsonet.keyple.demo.validation.domain.model.CardDescription
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.TechnicalError
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.reader.CardCommunicationException
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard

class CalypsoCardValidationProcedure(private val keypopApiProvider: KeypopApiProvider) :
    BaseValidationProcedure() {

  override fun supports(card: SmartCard): Boolean = card is CalypsoCard

  override fun execute(context: ValidationContext): ValidationResult {
    val calypsoCard = context.card as CalypsoCard
    val card = CardDescription.Calypso(HexUtil.toHex(calypsoCard.dfName))
    val cardSecuritySettings =
        checkNotNull(context.cardSecuritySetting) { "No SAM available for the Calypso cards" }

    // Create a card transaction for validation.
    val cardTransaction =
        try {
          keypopApiProvider
              .getCalypsoCardApiFactory()
              .createSecureRegularModeTransactionManager(
                  context.cardReader, calypsoCard, cardSecuritySettings)
        } catch (e: Exception) {
          return ValidationResult.Failed(card, TechnicalError.UNEXPECTED, e.message)
        }

    val outcome =
        try {
          validate(context, calypsoCard, card, cardTransaction)
        } catch (e: CardCommunicationException) {
          SessionOutcome(ValidationResult.CardLost(card), closeSession = false)
        } catch (e: Exception) {
          SessionOutcome(
              ValidationResult.Failed(card, TechnicalError.UNEXPECTED, e.message),
              closeSession = false)
        }
    var result = outcome.result

    // Step 14 - END: Close the session when the event has to be written (validated card, or
    // contract priorities updated for a refused card), cancel it otherwise
    try {
      if (outcome.closeSession) {
        cardTransaction.prepareCloseSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
      } else {
        cardTransaction.prepareCancelSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
      }
    } catch (e: CardCommunicationException) {
      // Card removed during close/cancel session (e.g. removed while writing)
      result = ValidationResult.CardLost(card)
    } catch (e: Exception) {
      if (result !is ValidationResult.CardLost) {
        result = ValidationResult.Failed(card, TechnicalError.UNEXPECTED, e.message)
      }
    }
    return result
  }

  /**
   * Result of the steps 1 to 13 of the validation procedure.
   *
   * @property closeSession Indicates whether the secure session has to be closed to write the event
   *   prepared, or cancelled.
   */
  private data class SessionOutcome(val result: ValidationResult, val closeSession: Boolean)

  /**
   * Executes the steps 1 to 13 of the validation procedure, the secure session being opened at the
   * step 1 and closed by the caller.
   */
  private fun validate(
      context: ValidationContext,
      calypsoCard: CalypsoCard,
      card: CardDescription,
      cardTransaction: SecureRegularModeTransactionManager
  ): SessionOutcome {
    val validationDateTime = context.dateTime

    // Refusal of the card before any update: the session is cancelled
    fun rejected(reason: RejectionReason) =
        SessionOutcome(ValidationResult.Rejected(card, reason), closeSession = false)

    // ***************** Event and Environment Analysis
    // Step 1 - Open a Validation session reading the environment record.
    cardTransaction
        .prepareOpenSecureSession(WriteAccessLevel.DEBIT)
        .prepareReadRecords(
            CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER,
            1,
            1,
            CalypsoFiles.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
        .processCommands(ChannelControl.KEEP_OPEN)

    // Step 2 - Unpack environment structure from the binary present in the environment record.
    val efEnvironmentHolder = calypsoCard.getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER)
    val environment = CalypsoEnvironmentHolderCodec.decode(efEnvironmentHolder.data.content)

    // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for
    // the
    // current version), reject the card. <Abort Secure Session>
    // Step 4 - If EnvEndDate points to a date in the past, reject the card. <Abort Secure Session>
    checkEnvironment(
            environment.envVersionNumber,
            environment.envEndDate.date,
            validationDateTime.toLocalDate())
        ?.let {
          return rejected(it)
        }

    // Step 5 - Read and unpack the last event record.
    cardTransaction
        .prepareReadRecords(CalypsoFiles.SFI_EVENTS_LOG, 1, 1, CalypsoFiles.EVENT_RECORD_SIZE_BYTES)
        .processCommands(ChannelControl.KEEP_OPEN)

    val efEventLog = calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENTS_LOG)
    val event = CalypsoEventCodec.decode(efEventLog.data.content)

    // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version), reject
    // the card. <Abort Secure Session>
    checkEventVersion(event.eventVersionNumber)?.let {
      return rejected(it)
    }

    // Step 6.2 - anti-passback management and communication failure recovery: within the
    // anti-passback delay, the card is refused if the last validation session has been ratified,
    // otherwise this session, interrupted by the removal of the card, is considered as validated.
    if (isWithinAntiPassbackDelay(event.eventDatetime, validationDateTime)) {
      return if (calypsoCard.isDfRatified) {
        rejected(RejectionReason.ALREADY_VALIDATED)
      } else {
        SessionOutcome(
            ValidationResult.Accepted(card, validationDateTime, validationData = null),
            closeSession = true)
      }
    }

    // ***************** Best Contract Search
    // Step 7 - Create a list of PriorityCode fields that are different from FORBIDDEN, EXPIRED and
    // UNKNOWN.
    val allPriorities =
        event.contractPriorities.mapIndexed { index, priority -> Pair(index + 1, priority) }
    val filteredPriorities = filterValidContractPriorities(allPriorities)

    // Step 9 - If the list is empty, go to END.
    if (filteredPriorities.isEmpty()) {
      return rejected(RejectionReason.NO_VALID_CONTRACT)
    }

    // Contract priorities of the event to write, updated by the validation
    val priorities = event.contractPriorities.toMutableList()
    var contractUsed = 0
    var writeEvent = false
    // Reason of the refusal of the last contract analyzed, if no contract can be used
    var rejectionReason: RejectionReason? = null
    var passValidityEndDate: LocalDate? = null
    var remainingTrips: Int? = null

    // Step 10 - For each element in the list:
    val sortedPriorities = sortContractPrioritiesByPriority(filteredPriorities)

    // Step 11 - For each element in the list:
    for ((record, contractPriority) in sortedPriorities) {

      // Step 11.1 - Read and unpack the contract record for the index being iterated.
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_CONTRACTS, record, record, CalypsoFiles.CONTRACT_RECORD_SIZE_BYTES)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efContractParser = calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS)
      val contractContent = efContractParser.data.allRecordsContent[record]!!
      val contract = CalypsoContractCodec.decode(contractContent)

      // Step 11.2 - If ContractVersionNumber is not the expected one (==1 for the current version),
      // reject the card. <Abort Secure Session>
      checkContractVersion(contract.contractVersionNumber)?.let {
        return rejected(it)
      }

      // Step 11.3 - ' If ContractAuthenticator is not 0, perform the verification of the value by
      // using the PSO Verify Signature command of the SAM.
      @Suppress("ControlFlowWithEmptyBody")
      if (contract.contractAuthenticator != 0) {
        // Step 11.3.1 - If the value is wrong reject the card. <Abort Secure Session>
        // Step 11.3.2 - If the value of ContractSaleSam is present in the SAM Black List reject the
        // card. <Abort Secure Session>
        // TODO: steps 11.3.1 & 11.3.2
      }

      // Step 11.4 - If ContractValidityEndDate points to a date in the past, update the associated
      // ContractPriority field present in the persistent object to 31 and move to the next element
      // in the list
      if (isContractExpired(
          contract.contractValidityEndDate.date, validationDateTime.toLocalDate())) {
        priorities[record - 1] = PriorityCode.EXPIRED
        rejectionReason = RejectionReason.EXPIRED_CONTRACT
        writeEvent = true
        continue
      }

      // Step 11.5 - If the ContractTariff value for the contract read is 2:
      if (isCounterBasedContract(contractPriority)) {

        val nbContractRecords =
            when (calypsoCard.productType) {
              CalypsoCard.ProductType.BASIC -> 1
              CalypsoCard.ProductType.LIGHT -> 2
              else -> 4
            }

        // Step 11.5.1 - Read and unpack the counter associated with the contract (1st counter for
        // Contract #1 and so forth).
        cardTransaction
            .prepareReadCounter(CalypsoFiles.SFI_COUNTERS, nbContractRecords)
            .processCommands(ChannelControl.KEEP_OPEN)

        val efCounter = calypsoCard.getFileBySfi(CalypsoFiles.SFI_COUNTERS)
        val counterValue = efCounter.data.getContentAsCounterValue(record)

        // Step 11.5.2 - If the counter-value is 0, update the associated ContractPriority field
        // present in the persistent object to 31 and move to the next element in the list
        if (!hasTripsLeft(counterValue)) {
          priorities[record - 1] = PriorityCode.EXPIRED
          rejectionReason = RejectionReason.NO_TRIPS_LEFT
          writeEvent = true
          continue
        }

        // Step 11.5.3 - UPDATE COUNTER: Decrement the counter-value by 1.
        val decrement = calculateDecrementAmount(contractPriority)
        if (decrement > 0) {
          cardTransaction.prepareDecreaseCounter(CalypsoFiles.SFI_COUNTERS, record, decrement)
          remainingTrips = counterValue - decrement
        }
      } else if (contractPriority == PriorityCode.SEASON_PASS) {
        passValidityEndDate = contract.contractValidityEndDate.date
      }

      // We will create a new event for this contract
      contractUsed = record
      writeEvent = true
      break
    }

    if (!writeEvent) {
      return rejected(rejectionReason ?: RejectionReason.NO_VALID_CONTRACT)
    }

    val eventToWrite: EventStructure
    val result: ValidationResult
    if (contractUsed > 0) {
      // Create a new validation event
      eventToWrite =
          EventStructure(
              eventVersionNumber = VersionNumber.CURRENT_VERSION,
              eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
              eventTimeStamp = TimeCompact(validationDateTime),
              eventLocation = context.location.id,
              eventContractUsed = contractUsed,
              contractPriorities = priorities.toList())
      result =
          ValidationResult.Accepted(
              card = card,
              dateTime = validationDateTime,
              validationData = ValidationDataBuilder.buildFrom(eventToWrite, context.locations),
              remainingTrips = remainingTrips,
              passValidityEndDate = passValidityEndDate)
    } else {
      // Update old event's priorities: the card is refused, but its expired or exhausted
      // contracts are no longer analyzed by the next validations
      eventToWrite = event.copy(contractPriorities = priorities.toList())
      result = ValidationResult.Rejected(card, rejectionReason ?: RejectionReason.NO_VALID_CONTRACT)
    }

    // Step 13 - Pack the Event structure and append it to the event file
    val eventBytesToWrite = CalypsoEventCodec.encode(eventToWrite)
    cardTransaction.prepareUpdateRecord(CalypsoFiles.SFI_EVENTS_LOG, 1, eventBytesToWrite)
    return SessionOutcome(result, closeSession = true)
  }
}
