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
package org.calypsonet.keyple.demo.control.domain.procedures

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.calypsonet.keyple.demo.common.codecs.StorageCardContractCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEventCodec
import org.calypsonet.keyple.demo.common.constants.MifareClassicBlocks
import org.calypsonet.keyple.demo.common.constants.StorageCardBlocks
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.control.domain.builders.buildContract
import org.calypsonet.keyple.demo.control.domain.builders.buildValidation
import org.calypsonet.keyple.demo.control.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.control.domain.model.Contract
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.model.RejectionReason
import org.calypsonet.keyple.demo.control.domain.model.TechnicalError
import org.calypsonet.keyple.demo.control.domain.model.Validation
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.MifareClassicKeyType
import org.eclipse.keypop.storagecard.SCAuthenticationFailedException
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard

class StorageCardControlProcedure(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : ControlProcedure {

  override fun supports(card: SmartCard): Boolean = card is StorageCard

  override fun execute(context: ControlContext): ControlResult {
    val controlDateTime = context.dateTime
    val cardReader = context.cardReader
    val storageCard = context.card as StorageCard
    val locations = context.locations
    val controlLocation = context.location
    val validationPeriodMinutes = context.validationPeriodMinutes

    var validation: Validation? = null

    val storageCardApiFactory =
        checkNotNull(keypopApiProvider.getStorageCardApiFactory()) {
          "Storage card extension not available"
        }

    try {
      // Create a card transaction for control
      val cardTransaction =
          try {
            storageCardApiFactory.createStorageCardTransactionManager(cardReader, storageCard)
          } catch (e: Exception) {
            throw IllegalStateException("Error while creating the storage card transaction", e)
          }

      // Determine card type and authentication requirements
      val requiresAuthentication = storageCard.productType.hasAuthentication()
      val isMifareClassic = storageCard.productType == ProductType.MIFARE_CLASSIC_1K

      logger.d(
          "Reading the storage card [productType=${storageCard.productType.name}, requiresAuthentication=$requiresAuthentication, isMifareClassic=$isMifareClassic]")

      // ========= AUTHENTICATION PHASE (MIFARE Classic only) =========
      if (requiresAuthentication) {
        logger.d(
            "Authenticating the MIFARE Classic sector [sector=1, keyType=KEY_A, keyNumber=${MifareClassicBlocks.DEFAULT_KEY_NUMBER}]")
        cardTransaction.prepareMifareClassicAuthenticate(
            MifareClassicBlocks.SECTOR_1_AUTH_BLOCK,
            MifareClassicKeyType.KEY_A,
            MifareClassicBlocks.DEFAULT_KEY_NUMBER)
      }

      // ========= READ DATA =========
      // Step 2 - Read environment, event and contract structures based on card type
      if (isMifareClassic) {
        logger.d(
            "Reading the MIFARE Classic blocks [environmentBlock=${MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK}, contractBlock=${MifareClassicBlocks.CONTRACT_BLOCK}, eventBlock=${MifareClassicBlocks.EVENT_BLOCK}]")
        // MIFARE Classic: read individual 16-byte blocks
        cardTransaction
            .prepareReadBlocks(
                MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK,
                MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
            .prepareReadBlocks(
                MifareClassicBlocks.CONTRACT_BLOCK, MifareClassicBlocks.CONTRACT_BLOCK)
            .prepareReadBlocks(MifareClassicBlocks.EVENT_BLOCK, MifareClassicBlocks.EVENT_BLOCK)
            .processCommands(ChannelControl.KEEP_OPEN)
      } else {
        logger.d("Reading the storage card block ranges")
        // MIFARE Ultralight/ST25: read ranges of 4-byte blocks
        cardTransaction
            .prepareReadBlocks(
                StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
            .prepareReadBlocks(
                StorageCardBlocks.EVENT_FIRST_BLOCK, StorageCardBlocks.EVENT_LAST_BLOCK)
            .prepareReadBlocks(
                StorageCardBlocks.CONTRACT_FIRST_BLOCK, StorageCardBlocks.COUNTER_LAST_BLOCK)
            .processCommands(ChannelControl.KEEP_OPEN)
      }

      logger.d("Storage card read")

      // Step 2 - Unpack environment structure
      val environmentContent =
          if (isMifareClassic) {
            storageCard.getBlock(MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
          } else {
            storageCard.getBlocks(
                StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
          }
      val environment = StorageCardEnvironmentHolderCodec.decode(environmentContent)

      // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for
      // the current version), reject the card.
      if (environment.envVersionNumber != VersionNumber.CURRENT_VERSION) {
        return ControlResult.Rejected(RejectionReason.ENVIRONMENT_WRONG_VERSION)
      }

      // Step 4 - If EnvEndDate points to a date in the past, reject the card.
      if (environment.envEndDate.date.isBefore(controlDateTime.toLocalDate())) {
        return ControlResult.Rejected(RejectionReason.ENVIRONMENT_EXPIRED)
      }

      // Step 5 - Read and unpack the event record
      val eventContent =
          if (isMifareClassic) {
            storageCard.getBlock(MifareClassicBlocks.EVENT_BLOCK)
          } else {
            storageCard.getBlocks(
                StorageCardBlocks.EVENT_FIRST_BLOCK, StorageCardBlocks.EVENT_LAST_BLOCK)
          }
      val event = StorageCardEventCodec.decode(eventContent)

      // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version),
      // reject
      // the card (if ==0 return error status indicating clean card).
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION) {
        return if (eventVersionNumber == VersionNumber.UNDEFINED) {
          ControlResult.EmptyCard
        } else {
          ControlResult.Rejected(RejectionReason.EVENT_WRONG_VERSION)
        }
      }

      var isEventValid = true
      val contractUsed = event.eventContractUsed

      val eventValidityEndDate = event.eventDateTime.plusMinutes(validationPeriodMinutes.toLong())

      // Step 7 - If EventLocation != value configured in the control terminal, set the validated
      // contract validity flag as false and go to the point CNT_READ.
      if (controlLocation.id != event.eventLocation) {
        isEventValid = false
      }
      // Step 8 - Else If EventDateStamp points to a date in the past
      // -> set the validated contract validity flag as false and go to point CNT_READ.
      else if (event.eventDateTime.isBefore(controlDateTime.toLocalDate().atStartOfDay())) {
        isEventValid = false
      }

      // Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) <
      // current time of the control terminal
      // -> set the validated contract valid flag as false.
      // The time of the event being recorded to the minute, the times are compared to the minute:
      // the validation is valid until the end of the minute of the end of the validation period
      else if (eventValidityEndDate.isBefore(controlDateTime.truncatedTo(ChronoUnit.MINUTES))) {
        isEventValid = false
      }

      // Step 10 - CNT_READ: Read contract data (already read above)
      val contractContent =
          if (isMifareClassic) {
            storageCard.getBlock(MifareClassicBlocks.CONTRACT_BLOCK)
          } else {
            storageCard.getBlocks(
                StorageCardBlocks.CONTRACT_FIRST_BLOCK, StorageCardBlocks.COUNTER_LAST_BLOCK)
          }
      val contract = StorageCardContractCodec.decode(contractContent)

      // Create validation if the event is valid
      if (isValidEvent(event)) {
        validation = buildValidation(event = event, contract = contract, locations = locations)
      }

      val displayedContracts = mutableListOf<Contract>()
      val recordNumber = 1 // Storage card has only one contract
      var isContractExpired = false
      var isContractValidated = false

      if (contract.contractVersionNumber == VersionNumber.UNDEFINED) {
        // Step 13 - If the ContractVersionNumber == 0, then the contract is blank
      } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
        // Step 14 - If ContractVersionNumber is not the expected one (==1 for the current
        // version), reject the card.
        return ControlResult.Rejected(RejectionReason.CONTRACT_WRONG_VERSION)
      } else {
        // Step 15 - If ContractAuthenticator is not 0, perform the verification
        @Suppress("ControlFlowWithEmptyBody")
        if (contract.contractAuthenticator != 0) {
          // Step 15.1 & 15.2 - TODO: SAM verification steps
        }

        // Step 16 - If ContractValidityEndDate points to a date in the past mark contract as
        // expired.
        if (contract.contractValidityEndDate.date.isBefore(controlDateTime.toLocalDate())) {
          isContractExpired = true
        }

        // Step 17 - If EventContractUsed points to the current contract index
        // and not the validity flag is false, then mark it as Validated.
        if (contractUsed == recordNumber && isEventValid) {
          isContractValidated = true
        }

        var validationDateTime: LocalDateTime? = null
        if (isContractValidated) {
          validationDateTime = event.eventDateTime
        }

        // Step 18 - If the ContractTariff value for the contract is 2, extract the counter-value.
        val remainingTrips =
            if (contract.contractTariff == PriorityCode.MULTI_TRIP) {
              contract.counterValue
            } else {
              null
            }

        // Step 19 - Add contract data to the list of contracts read to return to the upper layer.
        displayedContracts.add(
            buildContract(
                contract = contract,
                recordNumber = recordNumber,
                isContractExpired = isContractExpired,
                isContractValidated = isContractValidated,
                validationDateTime = validationDateTime,
                remainingTrips = remainingTrips))
      }

      // Step 20 - Close the transaction
      cardTransaction.processCommands(ChannelControl.CLOSE_AFTER)

      // Step 21 - Return the status of the operation to the upper layer. <Exit process>
      return ControlResult.CardContent(
          AuthenticationMode.NO_AUTHENTICATION, displayedContracts, validation)
    } catch (e: Exception) {
      logger.e(
          "Error while controlling the storage card [productType=${storageCard.productType.name}]",
          e)
      return ControlResult.Failed(technicalError(storageCard.productType, e))
    }
  }

  /** Indicates whether the exception, or one of its causes, is a failure of the authentication. */
  private fun isAuthenticationFailure(e: Exception): Boolean =
      generateSequence<Throwable>(e) { it.cause }.any { it is SCAuthenticationFailedException }

  /** Returns the technical error corresponding to the provided exception. */
  private fun technicalError(productType: ProductType, e: Exception): TechnicalError =
      when {
        // Specific error handling for MIFARE Classic authentication failures
        productType.hasAuthentication() && isAuthenticationFailure(e) ->
            TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED
        productType.hasAuthentication() -> TechnicalError.MIFARE_CLASSIC_READING_FAILED
        else -> TechnicalError.UNEXPECTED
      }

  /**
   * An event is considered valid for display if an eventTimeStamp or an eventDateStamp has been set
   * during a previous validation
   */
  private fun isValidEvent(event: EventStructure): Boolean {
    return event.eventTimeStamp.value != 0 || event.eventDateStamp.value != 0
  }
}
