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
package org.calypsonet.keyple.demo.inspection.domain.procedures

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.codecs.CalypsoContractCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEventCodec
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.inspection.domain.builders.buildContract
import org.calypsonet.keyple.demo.inspection.domain.builders.buildValidation
import org.calypsonet.keyple.demo.inspection.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.inspection.domain.model.Contract
import org.calypsonet.keyple.demo.inspection.domain.model.InspectionResult
import org.calypsonet.keyple.demo.inspection.domain.model.RejectionReason
import org.calypsonet.keyple.demo.inspection.domain.model.TechnicalError
import org.calypsonet.keyple.demo.inspection.domain.model.Validation
import org.calypsonet.keyple.demo.inspection.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.inspection.domain.spi.Logger
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SecurePkiModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.TransactionManager
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Inspection procedure of the Calypso cards. The steps referenced by the comments are described in
 * the annex "Card procedures" of the functional documentation
 * (docs/functional-documentation/index.adoc, section "Inspection").
 */
class CalypsoCardInspectionProcedure(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : BaseInspectionProcedure() {

  override fun supports(card: SmartCard): Boolean = card is CalypsoCard

  override fun execute(context: InspectionContext): InspectionResult {
    val inspectionDateTime = context.dateTime
    val cardReader = context.cardReader
    val calypsoCard = context.card as CalypsoCard
    val symmetricCryptoSecuritySetting = context.symmetricCryptoSecuritySetting
    val asymmetricCryptoSecuritySetting = context.asymmetricCryptoSecuritySetting
    val locations = context.locations
    val inspectionLocation = context.location
    val validationPeriodMinutes = context.validationPeriodMinutes

    var validation: Validation? = null

    val calypsoCardApiFactory = keypopApiProvider.getCalypsoCardApiFactory()

    // Steps 1.1 to 1.3: Authentication of the card by the SAM if available, otherwise by the PKI
    // mode if the card supports it, otherwise no authentication
    val authenticationMode: AuthenticationMode =
        if (symmetricCryptoSecuritySetting != null) AuthenticationMode.SAM
        else if (calypsoCard.isPkiModeSupported) AuthenticationMode.PKI
        else AuthenticationMode.NO_AUTHENTICATION

    try {
      val cardTransaction: TransactionManager<*> =
          when (authenticationMode) {
            AuthenticationMode.SAM ->
                calypsoCardApiFactory.createSecureRegularModeTransactionManager(
                    cardReader, calypsoCard, symmetricCryptoSecuritySetting)
            AuthenticationMode.PKI ->
                calypsoCardApiFactory.createSecurePkiModeTransactionManager(
                    cardReader, calypsoCard, asymmetricCryptoSecuritySetting)
            else -> calypsoCardApiFactory.createFreeTransactionManager(cardReader, calypsoCard)
          }

      if (cardTransaction is SecureRegularModeTransactionManager) {
        // Step 1.1: Open a secure session at the Debit access level (SAM)
        cardTransaction.prepareOpenSecureSession(WriteAccessLevel.DEBIT)
      } else if (cardTransaction is SecurePkiModeTransactionManager) {
        // Step 1.2: Open a PKI secure session
        cardTransaction.prepareOpenSecureSession()
      }
      // Step 1.3 otherwise: the environment record is read without authentication

      // Step 2: Read and unpack environment structure from the binary present in the environment
      // record.
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER,
              1,
              1,
              CalypsoFiles.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efEnvironmentHolder = calypsoCard.getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER)
      val environment = CalypsoEnvironmentHolderCodec.decode(efEnvironmentHolder.data.content)

      // Step 3: If EnvVersionNumber of the Environment structure is not the expected one (==1 for
      // the current version), reject the card.
      // <Abort Secure Session if any>
      if (environment.envVersionNumber != VersionNumber.CURRENT_VERSION) {
        cancelSecureSession(cardTransaction)
        return InspectionResult.Rejected(RejectionReason.ENVIRONMENT_WRONG_VERSION)
      }

      // Step 4: If EnvEndDate points to a date in the past, reject the card.
      // <Abort Secure Session if any>
      if (environment.envEndDate.date.isBefore(inspectionDateTime.toLocalDate())) {
        cancelSecureSession(cardTransaction)
        return InspectionResult.Rejected(RejectionReason.ENVIRONMENT_EXPIRED)
      }

      // Step 5: Read and unpack the last event record.
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_EVENT_LOG, 1, 1, CalypsoFiles.EVENT_RECORD_SIZE_BYTES)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efEventLog = calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENT_LOG)
      val event = CalypsoEventCodec.decode(efEventLog.data.content)

      // Step 6: If EventVersionNumber is not the expected one (==1 for the current version),
      // reject the card (if ==0 return error status indicating clean card).
      // <Abort Secure Session if any>
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION) {
        cancelSecureSession(cardTransaction)
        return if (eventVersionNumber == VersionNumber.UNDEFINED) {
          InspectionResult.EmptyCard
        } else {
          InspectionResult.Rejected(RejectionReason.EVENT_WRONG_VERSION)
        }
      }

      val contractUsed = event.eventContractUsed

      // Steps 7 to 9: The contract used by the last validation is validated only if this
      // validation was made at the location of the inspection, the same day, and within the
      // validation period
      val isLastValidationValid =
          isLastValidationValid(
              event, inspectionLocation, inspectionDateTime, validationPeriodMinutes)

      val contractRecordCount =
          when (calypsoCard.productType) {
            CalypsoCard.ProductType.BASIC -> 1
            CalypsoCard.ProductType.LIGHT -> 2
            else -> 4
          }

      // Step 10 (CNT_READ): Read all contracts and the counter-file
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_CONTRACTS,
              1,
              contractRecordCount,
              CalypsoFiles.CONTRACT_RECORD_SIZE_BYTES)
          .prepareReadCounter(CalypsoFiles.SFI_COUNTERS, contractRecordCount)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efCounters = calypsoCard.getFileBySfi(CalypsoFiles.SFI_COUNTERS)

      val efContracts = calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS)
      val contracts = mutableMapOf<Int, ContractStructure>()

      // Step 11: For each contract:
      efContracts.data.allRecordsContent.forEach {
        // Step 12: Unpack the contract
        contracts[it.key] = CalypsoContractCodec.decode(it.value)
      }

      // Contract used by the last event, null if it is not present in the card
      val eventContract = contracts[event.eventContractUsed]

      if (isValidationEvent(event)) {
        validation = buildValidation(event = event, contract = eventContract, locations = locations)
      }

      val displayedContracts = mutableListOf<Contract>()
      contracts.forEach {
        val recordNumber = it.key
        val contract = it.value
        var isContractExpired = false
        var isContractValidated = false

        if (contract.contractVersionNumber == VersionNumber.UNDEFINED) {
          // Step 13: If the ContractVersionNumber == 0, then the contract is blank, move on to the
          // next contract.
        } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          // Step 14: If ContractVersionNumber is not the expected one (==1 for the current
          // version), reject the card.
          // <Abort Secure Session if any>
          cancelSecureSession(cardTransaction)
          return InspectionResult.Rejected(RejectionReason.CONTRACT_WRONG_VERSION)
        } else {
          // Step 15: If SAM available and ContractAuthenticator is not 0, perform the verification
          // of the value
          // by using the PSO Verify Signature command of the SAM.
          @Suppress("ControlFlowWithEmptyBody")
          if (contract.contractAuthenticator != 0) {
            // Step 15.1: If the value is wrong reject the card.
            // <Abort Secure Session if any>
            // Step 15.2: If the value of ContractSaleSam is present in the SAM Black List reject
            // the card.
            // <Abort Secure Session if any>
            // TODO: steps 15.1 and 15.2
          }
          // Step 16: If ContractValidityEndDate points to a date in the past mark contract as
          // expired.
          if (contract.contractValidityEndDate.date.isBefore(inspectionDateTime.toLocalDate())) {
            isContractExpired = true
          }

          // Step 17: If EventContractUsed points to the current contract index
          // and not the validity flag is false, then mark it as Validated.
          if (contractUsed == recordNumber && isLastValidationValid) {
            isContractValidated = true
          }

          var validationDateTime: LocalDateTime? = null
          if (isContractValidated) {
            validationDateTime = event.eventDateTime
          }

          // Step 18: If the ContractTariff value for the contract is 2, unpack the counter
          // associated with the contract to extract the counter-value.
          val remainingTrips =
              if (contract.contractTariff == PriorityCode.MULTI_TRIP) {
                efCounters.data.getContentAsCounterValue(recordNumber)
              } else {
                null
              }

          // Step 19: Add contract data to the list of contracts read to return to the upper layer.
          displayedContracts.add(
              buildContract(
                  contract = contract,
                  recordNumber = recordNumber,
                  isContractExpired = isContractExpired,
                  isContractValidated = isContractValidated,
                  validationDateTime = validationDateTime,
                  remainingTrips = remainingTrips))
        }
      }

      // Step 20: If a session is open, close the session
      if (cardTransaction is SecureRegularModeTransactionManager ||
          cardTransaction is SecurePkiModeTransactionManager) {
        cardTransaction.prepareCloseSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
      }

      // Step 21: Return the status of the operation to the upper layer. <Exit process>
      return InspectionResult.CardContent(authenticationMode, displayedContracts, validation)
    } catch (e: Exception) {
      logger.e("Error while inspecting the card", e)
      return InspectionResult.Failed(TechnicalError.UNEXPECTED)
    }
  }

  /** Cancels the secure session of the provided transaction, if any (rejected card). */
  private fun cancelSecureSession(cardTransaction: TransactionManager<*>) {
    if (cardTransaction is SecureRegularModeTransactionManager ||
        cardTransaction is SecurePkiModeTransactionManager) {
      cardTransaction.prepareCancelSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
    }
  }
}
