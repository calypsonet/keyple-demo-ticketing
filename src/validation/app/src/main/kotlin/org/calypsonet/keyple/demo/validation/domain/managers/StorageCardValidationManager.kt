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
package org.calypsonet.keyple.demo.validation.domain.managers

import java.time.LocalDate
import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.codecs.StorageCardContractCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.StorageCardEventCodec
import org.calypsonet.keyple.demo.common.constants.MifareClassicBlocks
import org.calypsonet.keyple.demo.common.constants.StorageCardBlocks
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.validation.domain.builders.ValidationDataBuilder
import org.calypsonet.keyple.demo.validation.domain.model.Status
import org.calypsonet.keyple.demo.validation.domain.model.ValidationData
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.storagecard.MifareClassicKeyType
import org.eclipse.keypop.storagecard.SCCardCommunicationException
import org.eclipse.keypop.storagecard.card.ProductType
import org.eclipse.keypop.storagecard.card.StorageCard

/**
 * Unified validation manager for all storage cards (MIFARE Ultralight, ST25, Mifare Classic).
 *
 * This manager adapts its behavior based on the card's ProductType characteristics:
 * - For cards requiring authentication (Mifare Classic): performs sector authentication before
 *   read/write
 * - Block layout adapts to card type (4-byte blocks for UL/ST25, 16-byte blocks for Mifare Classic)
 *
 * Block layouts:
 * - MIFARE Ultralight/ST25 SRT512: blocks 4-7 (Env), 8-11 (Contract), 12-15 (Event) [4 bytes each]
 * - Mifare Classic 1K: blocks 4 (Env), 5 (Contract), 6 (Event) [16 bytes each, sector 1]
 */
class StorageCardValidationManager(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : BaseValidationManager() {

  /**
   * Formats the card ProductType for user display. Examples: MIFARE_CLASSIC_1K → "Mifare Classic
   * 1K" MIFARE_ULTRALIGHT → "Mifare Ultralight" ST25_SRT512 → "ST25 SRT512"
   */
  // The "else" branch is kept for the product types that a newer version of the Keypop API may add
  @Suppress("REDUNDANT_ELSE_IN_WHEN")
  private fun formatCardType(productType: ProductType): String {
    return when (productType) {
      ProductType.MIFARE_CLASSIC_1K -> "Mifare Classic 1K"
      ProductType.MIFARE_CLASSIC_4K -> "Mifare Classic 4K"
      ProductType.MIFARE_ULTRALIGHT -> "Mifare Ultralight"
      ProductType.ST25_SRT512 -> "ST25 SRT512"
      else -> productType.name.replace('_', ' ') // Fallback: replace underscores
    }
  }

  fun executeValidationProcedure(
      validationDateTime: LocalDateTime,
      cardReader: CardReader,
      storageCard: StorageCard,
      locations: List<Location>,
      validationLocation: Location
  ): ValidationResult {
    var status: Status = Status.PROCESSING
    var errorMessage: String? = null
    var passValidityEndDate: LocalDate? = null
    var remainingTrips: Int? = null
    var validationData: ValidationData? = null

    val storageCardApiFactory =
        checkNotNull(keypopApiProvider.getStorageCardApiFactory()) {
          "Storage card extension not available"
        }

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
              .prepareReadBlocks(
                  MifareClassicBlocks.CONTRACT_BLOCK, MifareClassicBlocks.CONTRACT_BLOCK)
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

        // Step 3 - Validate environment version
        validateEnvironmentVersionOrThrow(environment.envVersionNumber)

        // Step 4 - Validate environment end date
        validateEnvironmentDateOrThrow(
            environment.envEndDate.date, validationDateTime.toLocalDate())

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
        validateEventVersionOrThrow(event.eventVersionNumber)

        validateAntiPassbackOrThrow(event.eventDatetime, validationDateTime, true)

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
        validateContractVersionOrThrow(contract.contractVersionNumber)

        // Check contract validity
        try {
          validateContractDateOrThrow(
              contract.contractValidityEndDate.date, validationDateTime.toLocalDate())
        } catch (e: ValidationException) {
          status = e.status
          errorMessage = e.message
          throw e
        }

        // ========= BUSINESS VALIDATION =========
        // Determine contract priority from contract tariff
        val contractPriority = contract.contractTariff

        var writeEvent: Boolean
        val contractUsed = 1 // For storage card, we only have one contract
        // Contract to write, updated by the validation (counter of a multi-trip contract)
        var contractToWrite = contract

        when (contractPriority) {
          PriorityCode.MULTI_TRIP -> {
            // Check if there are trips left
            val counterValue = contract.counterValue ?: 0
            validateTripsAvailableOrThrow(counterValue)

            // Decrement counter
            val newCounterValue = counterValue - calculateDecrementAmount(contractPriority)
            contractToWrite = contract.withCounterValue(newCounterValue)
            remainingTrips = newCounterValue

            writeEvent = true
          }
          PriorityCode.SEASON_PASS -> {
            passValidityEndDate = contract.contractValidityEndDate.date
            writeEvent = true
          }
          PriorityCode.FORBIDDEN,
          PriorityCode.EXPIRED,
          PriorityCode.UNKNOWN -> {
            throw ValidationException(EXCEPTION_CONTRACT_FORBIDDEN_OR_EXPIRED, Status.EMPTY_CARD)
          }
        }

        // ========= WRITE DATA =========
        if (writeEvent) {
          // Create a new validation event
          val eventToWrite =
              EventStructure(
                  eventVersionNumber = VersionNumber.CURRENT_VERSION,
                  eventDateStamp = DateCompact(validationDateTime.toLocalDate()),
                  eventTimeStamp = TimeCompact(validationDateTime),
                  eventLocation = validationLocation.id,
                  eventContractUsed = contractUsed,
                  contractPriorities =
                      listOf(
                          contractPriority,
                          PriorityCode.FORBIDDEN,
                          PriorityCode.FORBIDDEN,
                          PriorityCode.FORBIDDEN))

          validationData = ValidationDataBuilder.buildFrom(eventToWrite, locations)

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
          status = Status.SUCCESS
          errorMessage = null
        } else {
          errorMessage = ERROR_NO_VALID_CONTRACT_DETECTED
        }
      } catch (e: ValidationException) {
        logger.w("Validation failed: ${e.status.name} - ${e.message}")
        status = e.status
        errorMessage = e.message
        if (status == Status.PROCESSING) {
          status = Status.ERROR
        }
      } catch (e: SCCardCommunicationException) {
        logger.w("Card removed during transaction: ${storageCard.productType.name}")
        status = Status.CARD_LOST
        errorMessage = e.message
      } catch (e: Exception) {
        logger.e("Unexpected error during validation: ${storageCard.productType.name}", e)
        status = Status.ERROR

        // Determine the error message based on exception type and context
        errorMessage =
            when {
              // Specific authentication failure for Mifare Classic
              storageCard.productType.hasAuthentication() &&
                  (e.message?.contains("authentication", ignoreCase = true) == true ||
                      e.message?.contains("auth", ignoreCase = true) == true) -> {
                ERROR_MIFARE_CLASSIC_AUTH_FAILED
              }
              // Generic Mifare Classic transaction failure
              storageCard.productType.hasAuthentication() -> {
                ERROR_MIFARE_CLASSIC_TRANSACTION_FAILED
              }
              // Other storage card errors
              else -> e.message ?: ERROR_GENERIC_TRANSACTION_FAILED
            }
      }
    } else {
      logger.e("Failed to create card transaction")
    }

    return ValidationResult(
        status = status,
        cardType = formatCardType(storageCard.productType),
        remainingTrips = remainingTrips,
        contract = EMPTY_CONTRACT,
        validationData = validationData,
        errorMessage = errorMessage,
        passValidityEndDate = passValidityEndDate,
        eventDateTime = validationDateTime)
  }
}
