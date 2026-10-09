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
import org.calypsonet.keyple.demo.validation.domain.builders.ValidationDataBuilder
import org.calypsonet.keyple.demo.validation.domain.model.CardDescription
import org.calypsonet.keyple.demo.validation.domain.model.RejectionReason
import org.calypsonet.keyple.demo.validation.domain.model.TechnicalError
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.MifareClassicKeyType
import org.eclipse.keypop.storagecard.SCCardCommunicationException
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard
import org.eclipse.keypop.storagecard.transaction.StorageCardTransactionManager

/**
 * Unified validation procedure for all storage cards (MIFARE Ultralight, ST25, Mifare Classic).
 *
 * This procedure adapts its behavior based on the card's ProductType characteristics:
 * - For cards requiring authentication (Mifare Classic): performs sector authentication before
 *   read/write
 * - Block layout adapts to card type (4-byte blocks for UL/ST25, 16-byte blocks for Mifare Classic)
 *
 * Block layouts:
 * - MIFARE Ultralight/ST25 SRT512: blocks 4-7 (Env), 8-11 (Contract), 12-15 (Event) [4 bytes each]
 * - Mifare Classic 1K: blocks 4 (Env), 5 (Contract), 6 (Event) [16 bytes each, sector 1]
 */
class StorageCardValidationProcedure(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : BaseValidationProcedure() {

  override fun supports(card: SmartCard): Boolean = card is StorageCard

  override fun execute(context: ValidationContext): ValidationResult {
    val storageCard = context.card as StorageCard
    val card = CardDescription.Storage(storageCard.productType.name)

    val storageCardApiFactory =
        checkNotNull(keypopApiProvider.getStorageCardApiFactory()) {
          "Storage card extension not available"
        }

    // Create a card transaction for validation
    val cardTransaction =
        try {
          storageCardApiFactory.createStorageCardTransactionManager(context.cardReader, storageCard)
        } catch (e: Exception) {
          logger.e("Failed to create card transaction")
          return ValidationResult.Failed(card, TechnicalError.UNEXPECTED, e.message)
        }

    return try {
      validate(context, storageCard, card, cardTransaction).also {
        if (it is ValidationResult.Rejected) {
          logger.w("Validation failed: ${it.reason}")
        }
      }
    } catch (e: SCCardCommunicationException) {
      logger.w("Card removed during transaction: ${storageCard.productType.name}")
      ValidationResult.CardLost(card)
    } catch (e: Exception) {
      logger.e("Unexpected error during validation: ${storageCard.productType.name}", e)
      ValidationResult.Failed(card, technicalError(storageCard.productType, e), e.message)
    }
  }

  /** Returns the technical error corresponding to the provided exception. */
  private fun technicalError(productType: ProductType, e: Exception): TechnicalError =
      when {
        // Specific authentication failure for Mifare Classic
        productType.hasAuthentication() && e.message?.contains("auth", ignoreCase = true) == true ->
            TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED
        // Generic Mifare Classic transaction failure
        productType.hasAuthentication() -> TechnicalError.MIFARE_CLASSIC_TRANSACTION_FAILED
        // Other storage card errors
        else -> TechnicalError.UNEXPECTED
      }

  /** Reads the card, applies the validation rules and writes the validation in the card. */
  private fun validate(
      context: ValidationContext,
      storageCard: StorageCard,
      card: CardDescription,
      cardTransaction: StorageCardTransactionManager
  ): ValidationResult {
    val validationDateTime = context.dateTime

    // Determine if this card requires authentication (Mifare Classic)
    val requiresAuth = storageCard.productType.hasAuthentication()
    val isMifareClassic = storageCard.productType == ProductType.MIFARE_CLASSIC_1K

    // LOG: Card detected
    logger.d(
        "Starting validation for ${storageCard.productType.name} " +
            "(requiresAuth=$requiresAuth, isMifareClassic=$isMifareClassic)")

    // ========= AUTHENTICATION PHASE (Mifare Classic only) =========
    if (requiresAuth) {
      logger.d(
          "Authenticating sector 1 with KEY_A (keyNumber=${MifareClassicBlocks.DEFAULT_KEY_NUMBER})")
      cardTransaction.prepareMifareClassicAuthenticate(
          MifareClassicBlocks.SECTOR_1_AUTH_BLOCK,
          MifareClassicKeyType.KEY_A,
          MifareClassicBlocks.DEFAULT_KEY_NUMBER)
    }

    // ========= READ DATA =========
    // Read environment, contract, and event based on card type
    if (isMifareClassic) {
      logger.d(
          "Reading Mifare Classic blocks: ${MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK}, " +
              "${MifareClassicBlocks.CONTRACT_BLOCK}, ${MifareClassicBlocks.EVENT_BLOCK}")
      // Mifare Classic: read individual 16-byte blocks
      cardTransaction
          .prepareReadBlocks(
              MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK,
              MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
          .prepareReadBlocks(MifareClassicBlocks.CONTRACT_BLOCK, MifareClassicBlocks.CONTRACT_BLOCK)
          .prepareReadBlocks(MifareClassicBlocks.EVENT_BLOCK, MifareClassicBlocks.EVENT_BLOCK)
          .processCommands(ChannelControl.KEEP_OPEN)
    } else {
      logger.d("Reading storage card block ranges...")
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

    logger.d("Card data read successfully")

    // Step 2 - Unpack environment structure (16 bytes regardless of card type)
    val environmentContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.ENVIRONMENT_AND_HOLDER_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
              StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK)
        }
    val environment = StorageCardEnvironmentHolderCodec.decode(environmentContent)

    // Steps 3 and 4 - Validate the environment version and end date
    checkEnvironment(
            environment.envVersionNumber,
            environment.envEndDate.date,
            validationDateTime.toLocalDate())
        ?.let {
          return ValidationResult.Rejected(card, it)
        }

    // Step 5 - Read and unpack the event record (16 bytes)
    val eventContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.EVENT_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.EVENT_FIRST_BLOCK, StorageCardBlocks.EVENT_LAST_BLOCK)
        }
    val event = StorageCardEventCodec.decode(eventContent)

    // Step 6 - Validate the event version
    checkEventVersion(event.eventVersionNumber)?.let {
      return ValidationResult.Rejected(card, it)
    }

    // Anti-passback: the storage cards have no session to recover
    if (isWithinAntiPassbackDelay(event.eventDatetime, validationDateTime)) {
      return ValidationResult.Rejected(card, RejectionReason.ALREADY_VALIDATED)
    }

    // Step 7 - Read and unpack the contract record (16 bytes)
    val contractContent =
        if (isMifareClassic) {
          storageCard.getBlock(MifareClassicBlocks.CONTRACT_BLOCK)
        } else {
          storageCard.getBlocks(
              StorageCardBlocks.CONTRACT_FIRST_BLOCK, StorageCardBlocks.COUNTER_LAST_BLOCK)
        }
    val contract = StorageCardContractCodec.decode(contractContent)

    // Validate contract version
    checkContractVersion(contract.contractVersionNumber)?.let {
      return ValidationResult.Rejected(card, it)
    }

    // Check contract validity
    if (isContractExpired(
        contract.contractValidityEndDate.date, validationDateTime.toLocalDate())) {
      return ValidationResult.Rejected(card, RejectionReason.EXPIRED_CONTRACT)
    }

    // ========= BUSINESS VALIDATION =========
    // Determine contract priority from contract tariff
    val contractPriority = contract.contractTariff

    val contractUsed = 1 // For storage card, we only have one contract
    // Contract to write, updated by the validation (counter of a multi-trip contract)
    var contractToWrite = contract
    var remainingTrips: Int? = null
    var passValidityEndDate: LocalDate? = null

    when (contractPriority) {
      PriorityCode.MULTI_TRIP -> {
        // Check if there are trips left
        val counterValue = contract.counterValue ?: 0
        if (!hasTripsLeft(counterValue)) {
          return ValidationResult.Rejected(card, RejectionReason.NO_TRIPS_LEFT)
        }

        // Decrement counter
        val newCounterValue = counterValue - calculateDecrementAmount(contractPriority)
        contractToWrite = contract.withCounterValue(newCounterValue)
        remainingTrips = newCounterValue
      }
      PriorityCode.SEASON_PASS -> {
        passValidityEndDate = contract.contractValidityEndDate.date
      }
      PriorityCode.FORBIDDEN,
      PriorityCode.EXPIRED,
      PriorityCode.UNKNOWN -> {
        return ValidationResult.Rejected(card, RejectionReason.CONTRACT_FORBIDDEN_OR_EXPIRED)
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
            contractPriorities =
                listOf(
                    contractPriority,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN,
                    PriorityCode.FORBIDDEN))

    val validationData = ValidationDataBuilder.buildFrom(eventToWrite, context.locations)

    // Re-authenticate for Mifare Classic before writing (best practice)
    if (requiresAuth) {
      logger.d("Re-authenticating before write operation")
      cardTransaction.prepareMifareClassicAuthenticate(
          MifareClassicBlocks.SECTOR_1_AUTH_BLOCK,
          MifareClassicKeyType.KEY_A,
          MifareClassicBlocks.DEFAULT_KEY_NUMBER)
    }

    logger.d("Writing updated contract and event to card")
    // Prepare write operations based on card type
    val updatedContractContent = StorageCardContractCodec.encode(contractToWrite)
    val eventBytesToWrite = StorageCardEventCodec.encode(eventToWrite)

    if (isMifareClassic) {
      // Mifare Classic: write individual blocks
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

    logger.i("Validation successful: ${storageCard.productType.name}")
    return ValidationResult.Accepted(
        card = card,
        dateTime = validationDateTime,
        validationData = validationData,
        remainingTrips = remainingTrips,
        passValidityEndDate = passValidityEndDate)
  }
}
