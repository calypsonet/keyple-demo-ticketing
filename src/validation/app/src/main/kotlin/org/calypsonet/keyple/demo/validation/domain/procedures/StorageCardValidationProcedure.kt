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
import org.calypsonet.keyple.demo.common.codecs.StorageCardContractCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEventCodec
import org.calypsonet.keyple.demo.common.constants.MifareClassicBlocks
import org.calypsonet.keyple.demo.common.constants.StorageCardBlocks
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.domain.model.CardDescription
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.TechnicalError
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.MifareClassicKeyType
import org.eclipse.keypop.storagecard.SCAuthenticationFailedException
import org.eclipse.keypop.storagecard.SCCardCommunicationException
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard
import org.eclipse.keypop.storagecard.transaction.StorageCardTransactionManager

/**
 * Unified validation procedure for all storage cards (MIFARE Ultralight, ST25, MIFARE Classic).
 *
 * This procedure adapts its behavior based on the card's ProductType characteristics:
 * - For cards requiring authentication (MIFARE Classic): performs sector authentication before
 *   read/write
 * - Block layout adapts to card type (4-byte blocks for UL/ST25, 16-byte blocks for MIFARE Classic)
 *
 * Block layouts:
 * - MIFARE Ultralight/ST25 SRT512: blocks 4-7 (Env), 8-11 (Contract), 12-15 (Event) [4 bytes each]
 * - MIFARE Classic 1K: blocks 4 (Env), 5 (Contract), 6 (Event) [16 bytes each, sector 1]
 */
class StorageCardValidationProcedure(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : BaseValidationProcedure() {

  override fun supports(card: SmartCard): Boolean = card is StorageCard

  override fun execute(context: ValidationContext): ValidationResult {
    val storageCard = context.card as StorageCard
    val cardDescription = CardDescription.Storage(storageCard.productType.name)

    val storageCardApiFactory =
        checkNotNull(keypopApiProvider.getStorageCardApiFactory()) {
          "Storage card extension not available"
        }

    // Create a card transaction for validation
    val cardTransaction =
        try {
          storageCardApiFactory.createStorageCardTransactionManager(context.cardReader, storageCard)
        } catch (e: Exception) {
          logger.e("Error while creating the storage card transaction", e)
          return ValidationResult.Failed(cardDescription, TechnicalError.UNEXPECTED)
        }

    return try {
      validate(context, storageCard, cardDescription, cardTransaction)
    } catch (e: SCCardCommunicationException) {
      // Card removal logged by the use case
      ValidationResult.CardRemoved(cardDescription)
    } catch (e: Exception) {
      logger.e(
          "Unexpected error while validating the storage card [productType=${storageCard.productType.name}]",
          e)
      ValidationResult.Failed(cardDescription, technicalError(storageCard.productType, e))
    }
  }

  /** Indicates whether the exception, or one of its causes, is a failure of the authentication. */
  private fun isAuthenticationFailure(e: Exception): Boolean =
      generateSequence<Throwable>(e) { it.cause }.any { it is SCAuthenticationFailedException }

  /** Returns the technical error corresponding to the provided exception. */
  private fun technicalError(productType: ProductType, e: Exception): TechnicalError =
      when {
        // Specific authentication failure for MIFARE Classic
        productType.hasAuthentication() && isAuthenticationFailure(e) ->
            TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED
        // Generic MIFARE Classic transaction failure
        productType.hasAuthentication() -> TechnicalError.MIFARE_CLASSIC_TRANSACTION_FAILED
        // Other storage card errors
        else -> TechnicalError.UNEXPECTED
      }

  /** Reads the card, applies the validation rules, and writes the validation in the card. */
  private fun validate(
      context: ValidationContext,
      storageCard: StorageCard,
      cardDescription: CardDescription,
      cardTransaction: StorageCardTransactionManager
  ): ValidationResult {
    val validationDateTime = context.dateTime

    // Determine if this card requires authentication (MIFARE Classic)
    val requiresAuthentication = storageCard.productType.hasAuthentication()
    val isMifareClassic = storageCard.productType == ProductType.MIFARE_CLASSIC_1K

    // LOG: Card detected
    logger.d(
        "Validating the storage card [productType=${storageCard.productType.name}, requiresAuthentication=$requiresAuthentication, isMifareClassic=$isMifareClassic]")

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
    // Read environment, contract, and event based on card type
    if (isMifareClassic) {
      logger.d(
          "Reading the MIFARE Classic blocks [environmentBlock=${MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK}, contractBlock=${MifareClassicBlocks.CONTRACT_BLOCK}, eventBlock=${MifareClassicBlocks.EVENT_BLOCK}]")
      // MIFARE Classic: read individual 16-byte blocks
      cardTransaction
          .prepareReadBlocks(
              MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK,
              MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
          .prepareReadBlocks(MifareClassicBlocks.CONTRACT_BLOCK, MifareClassicBlocks.CONTRACT_BLOCK)
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

    // Step 2: Unpack environment structure (16 bytes regardless of card type)
    val environmentContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
              StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
        }
    val environment = StorageCardEnvironmentHolderCodec.decode(environmentContent)

    // Steps 3 and 4: Validate the environment version and end date
    environmentRejection(
            environment.envVersionNumber,
            environment.envEndDate.date,
            validationDateTime.toLocalDate())
        ?.let {
          return ValidationResult.Rejected(cardDescription, it)
        }

    // Step 5: Read and unpack the event record (16 bytes)
    val eventContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.EVENT_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.EVENT_FIRST_BLOCK, StorageCardBlocks.EVENT_LAST_BLOCK)
        }
    val event = StorageCardEventCodec.decode(eventContent)

    // Step 6: Validate the event version
    eventVersionRejection(event.eventVersionNumber)?.let {
      return ValidationResult.Rejected(cardDescription, it)
    }

    // Anti-passback: the storage cards have no session to ratify
    if (isWithinAntiPassbackDelay(event.eventDateTime, validationDateTime)) {
      return ValidationResult.Rejected(cardDescription, RejectionReason.ALREADY_VALIDATED)
    }

    // Step 7: Read and unpack the contract record (16 bytes)
    val contractContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.CONTRACT_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.CONTRACT_FIRST_BLOCK, StorageCardBlocks.COUNTER_LAST_BLOCK)
        }
    val contract = StorageCardContractCodec.decode(contractContent)

    // Validate contract version
    contractVersionRejection(contract.contractVersionNumber)?.let {
      return ValidationResult.Rejected(cardDescription, it)
    }

    // Check contract validity
    if (isContractExpired(
        contract.contractValidityEndDate.date, validationDateTime.toLocalDate())) {
      return ValidationResult.Rejected(cardDescription, RejectionReason.EXPIRED_CONTRACT)
    }

    // ========= BUSINESS VALIDATION =========
    val contractUsed = 1 // A storage card holds only one contract
    // Contract to write, updated by the validation (counter of a multi-trip contract)
    var contractToWrite = contract
    var remainingTrips: Int? = null
    var seasonPassValidityEndDate: LocalDate? = null

    when (contract.contractTariff) {
      PriorityCode.MULTI_TRIP -> {
        // Check if there are trips left
        val counterValue = contract.counterValue ?: 0
        if (!hasTripsLeft(counterValue)) {
          return ValidationResult.Rejected(cardDescription, RejectionReason.NO_TRIPS_LEFT)
        }

        // Decrement counter
        val newCounterValue = counterValue - calculateDecrementAmount(contract.contractTariff)
        contractToWrite = contract.withCounterValue(newCounterValue)
        remainingTrips = newCounterValue
      }
      PriorityCode.SEASON_PASS -> {
        seasonPassValidityEndDate = contract.contractValidityEndDate.date
      }
      PriorityCode.FORBIDDEN,
      PriorityCode.EXPIRED,
      PriorityCode.UNKNOWN -> {
        return ValidationResult.Rejected(
            cardDescription, RejectionReason.CONTRACT_FORBIDDEN_OR_EXPIRED)
      }
    }

    // ========= WRITE DATA =========
    // Create a new validation event
    val eventToWrite =
        EventStructure(
            eventVersionNumber = VersionNumber.CURRENT_VERSION,
            eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
            eventTimeStamp = TimeCompact(validationDateTime),
            eventLocation = context.location.id,
            eventContractUsed = contractUsed,
            // The priority of a contract being its tariff
            contractPriorities =
                listOf(
                    contract.contractTariff,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN))

    // Re-authenticate for MIFARE Classic before writing (best practice)
    if (requiresAuthentication) {
      logger.d("Authenticating the MIFARE Classic sector again before writing")
      cardTransaction.prepareMifareClassicAuthenticate(
          MifareClassicBlocks.SECTOR_1_AUTH_BLOCK,
          MifareClassicKeyType.KEY_A,
          MifareClassicBlocks.DEFAULT_KEY_NUMBER)
    }

    logger.d("Writing the contract and the event")
    // Prepare write operations based on card type
    val updatedContractContent = StorageCardContractCodec.encode(contractToWrite)
    val eventBytesToWrite = StorageCardEventCodec.encode(eventToWrite)

    if (isMifareClassic) {
      // MIFARE Classic: write individual blocks
      cardTransaction
          .prepareWriteBlocks(MifareClassicBlocks.CONTRACT_BLOCK, updatedContractContent)
          .prepareWriteBlocks(MifareClassicBlocks.EVENT_BLOCK, eventBytesToWrite)
    } else {
      // MIFARE Ultralight/ST25: write block ranges
      cardTransaction
          .prepareWriteBlocks(StorageCardBlocks.CONTRACT_FIRST_BLOCK, updatedContractContent)
          .prepareWriteBlocks(StorageCardBlocks.EVENT_FIRST_BLOCK, eventBytesToWrite)
    }

    cardTransaction.processCommands(ChannelControl.CLOSE_AFTER)

    return ValidationResult.Accepted(
        cardDescription = cardDescription,
        dateTime = validationDateTime,
        location = context.location,
        remainingTrips = remainingTrips,
        seasonPassValidityEndDate = seasonPassValidityEndDate)
  }
}
