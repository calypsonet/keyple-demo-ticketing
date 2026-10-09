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
import org.calypsonet.keyple.demo.common.codecs.CalypsoContractCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEnvironmentHolderCodec
import org.calypsonet.keyple.demo.common.codecs.CalypsoEventCodec
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import org.calypsonet.keyple.demo.control.domain.mappers.ContractMapper
import org.calypsonet.keyple.demo.control.domain.mappers.ValidationMapper
import org.calypsonet.keyple.demo.control.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.control.domain.model.Contract
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.model.RejectionReason
import org.calypsonet.keyple.demo.control.domain.model.TechnicalError
import org.calypsonet.keyple.demo.control.domain.model.Validation
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SecurePkiModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.TransactionManager
import org.eclipse.keypop.reader.ChannelControl
import org.eclipse.keypop.reader.selection.spi.SmartCard

class CalypsoCardControlProcedure(
    private val keypopApiProvider: KeypopApiProvider,
    private val logger: Logger
) : ControlProcedure {

  override fun supports(card: SmartCard): Boolean = card is CalypsoCard

  override fun execute(context: ControlContext): ControlResult {
    val controlDateTime = context.dateTime
    val cardReader = context.cardReader
    val calypsoCard = context.card as CalypsoCard
    val symmetricCryptoSecuritySetting = context.symmetricCryptoSecuritySetting
    val asymmetricCryptoSecuritySetting = context.asymmetricCryptoSecuritySetting
    val locations = context.locations
    val controlLocation = context.location
    val validationPeriod = context.validationPeriod

    var validation: Validation? = null

    val calypsoCardApiFactory = keypopApiProvider.getCalypsoCardApiFactory()

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
        // Open a transaction to read/write the Calypso Card and read the Environment file
        cardTransaction.prepareOpenSecureSession(WriteAccessLevel.DEBIT)
      } else if (cardTransaction is SecurePkiModeTransactionManager) {
        cardTransaction.prepareOpenSecureSession()
      }

      // Step 2 - Read and unpack environment structure from the binary present in the environment
      // record.
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER,
              1,
              1,
              CalypsoFiles.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efEnvironmentHolder = calypsoCard.getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER)
      val env = CalypsoEnvironmentHolderCodec.decode(efEnvironmentHolder.data.content)

      // Step 3 - If EnvVersionNumber of the Environment structure is not the expected one (==1 for
      // the current version), reject the card.
      // <Abort Secure Session if any>
      if (env.envVersionNumber != VersionNumber.CURRENT_VERSION) {
        cancelSecureSession(cardTransaction)
        return ControlResult.Rejected(RejectionReason.ENVIRONMENT_WRONG_VERSION)
      }

      // Step 4 - If EnvEndDate points to a date in the past, reject the card.
      // <Abort Secure Session if any>
      if (env.envEndDate.date.isBefore(controlDateTime.toLocalDate())) {
        cancelSecureSession(cardTransaction)
        return ControlResult.Rejected(RejectionReason.ENVIRONMENT_EXPIRED)
      }

      // Step 5 - Read and unpack the last event record.
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_EVENTS_LOG, 1, 1, CalypsoFiles.EVENT_RECORD_SIZE_BYTES)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efEventLog = calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENTS_LOG)
      val event = CalypsoEventCodec.decode(efEventLog.data.content)

      // Step 6 - If EventVersionNumber is not the expected one (==1 for the current version),
      // reject
      // the card (if ==0 return error status indicating clean card).
      // <Abort Secure Session if any>
      val eventVersionNumber = event.eventVersionNumber
      if (eventVersionNumber != VersionNumber.CURRENT_VERSION) {
        cancelSecureSession(cardTransaction)
        return if (eventVersionNumber == VersionNumber.UNDEFINED) {
          ControlResult.EmptyCard
        } else {
          ControlResult.Rejected(RejectionReason.EVENT_WRONG_VERSION)
        }
      }

      var contractEventValid = true
      val contractUsed = event.eventContractUsed

      val eventValidityEndDate = event.eventDatetime.plusMinutes(validationPeriod.toLong())

      // Step 7 - If EventLocation != value configured in the control terminal, set the validated
      // contract validity flag as false and go to the point CNT_READ.
      if (controlLocation.id != event.eventLocation) {
        contractEventValid = false
      }
      // Step 8 - Else If EventDateStamp points to a date in the past
      // -> set the validated contract validity flag as false and go to point CNT_READ.
      else if (event.eventDatetime.isBefore(controlDateTime.toLocalDate().atStartOfDay())) {
        contractEventValid = false
      }

      // Step 9 - Else If (EventTimeStamp + Validation period configure in the control terminal) <
      // current time of the control terminal
      // -> set the validated contract valid flag as false.
      else if (eventValidityEndDate.isBefore(controlDateTime)) {
        contractEventValid = false
      }

      val nbContractRecords =
          when (calypsoCard.productType) {
            CalypsoCard.ProductType.BASIC -> 1
            CalypsoCard.ProductType.LIGHT -> 2
            else -> 4
          }

      // Step 10 - CNT_READ: Read all contracts and the counter-file
      cardTransaction
          .prepareReadRecords(
              CalypsoFiles.SFI_CONTRACTS,
              1,
              nbContractRecords,
              CalypsoFiles.CONTRACT_RECORD_SIZE_BYTES)
          .prepareReadCounter(CalypsoFiles.SFI_COUNTERS, nbContractRecords)
          .processCommands(ChannelControl.KEEP_OPEN)

      val efCounters = calypsoCard.getFileBySfi(CalypsoFiles.SFI_COUNTERS)

      val efContracts = calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS)
      val contracts = mutableMapOf<Int, ContractStructure>()

      // Step 11 - For each contract:
      efContracts.data.allRecordsContent.forEach {
        // Step 12 - Unpack the contract
        contracts[it.key] = CalypsoContractCodec.decode(it.value)
      }

      // Retrieve contract used for the last event
      val eventContract =
          contracts.toList().filter { it.first == event.eventContractUsed }.map { it.second }

      if (isValidEvent(event)) {
        validation =
            if (eventContract.isNotEmpty()) {
              ValidationMapper.map(
                  event = event, contract = eventContract[0], locations = locations)
            } else {
              ValidationMapper.map(event = event, contract = null, locations = locations)
            }
      }

      val displayedContract = mutableListOf<Contract>()
      contracts.forEach {
        val record = it.key
        val contract = it.value
        var contractExpired = false
        var contractValidated = false

        if (contract.contractVersionNumber == VersionNumber.UNDEFINED) {
          // Step 13 - If the ContractVersionNumber == 0, then the contract is blank, move on to the
          // next contract.
        } else if (contract.contractVersionNumber != VersionNumber.CURRENT_VERSION) {
          // Step 14 - If ContractVersionNumber is not the expected one (==1 for the current
          // version), reject the card.
          // <Abort Secure Session if any>
        } else {
          // Step 15 - If SAM available and ContractAuthenticator is not 0, perform the verification
          // of the value
          // by using the PSO Verify Signature command of the SAM.
          @Suppress("ControlFlowWithEmptyBody")
          if (contract.contractAuthenticator != 0) {
            // Step 15.1 - If the value is wrong reject the card.
            // <Abort Secure Session if any>
            // Step 15.2 - If the value of ContractSaleSam is present in the SAM Black List reject
            // the card.
            // <Abort Secure Session if any>
            // TODO: steps 15.1 & 15.2
          }
          // Step 16 - If ContractValidityEndDate points to a date in the past mark contract as
          // expired.
          if (contract.contractValidityEndDate.date.isBefore(controlDateTime.toLocalDate())) {
            contractExpired = true
          }

          // Step 17 - If EventContractUsed points to the current contract index
          // and not the validity flag is false, then mark it as Validated.
          if (contractUsed == record && contractEventValid) {
            contractValidated = true
          }

          var validationDateTime: LocalDateTime? = null
          if (contractValidated) {
            validationDateTime = event.eventDatetime
          }

          // Step 18 - If the ContractTariff value for the contract is 2, unpack the counter
          // associated with the contract to extract the counter-value.
          val remainingTrips =
              if (contract.contractTariff == PriorityCode.MULTI_TRIP) {
                efCounters.data.getContentAsCounterValue(record)
              } else {
                null
              }

          // Step 19 - Add contract data to the list of contracts read to return to the upper layer.
          displayedContract.add(
              ContractMapper.map(
                  contract = contract,
                  record = record,
                  contractExpired = contractExpired,
                  contractValidated = contractValidated,
                  validationDateTime = validationDateTime,
                  remainingTrips = remainingTrips))
        }
      }

      logger.i("Control procedure result: STATUS_OK")

      // Step 20 - If a session is open, Close the session
      if (cardTransaction is SecureRegularModeTransactionManager ||
          cardTransaction is SecurePkiModeTransactionManager) {
        cardTransaction.prepareCloseSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
      }

      // Step 21 - Return the status of the operation to the upper layer. <Exit process>
      return ControlResult.CardContent(authenticationMode, displayedContract, validation)
    } catch (e: Exception) {
      logger.e("Control procedure error: ${e.message}")
      return ControlResult.Failed(TechnicalError.UNEXPECTED, e.message)
    }
  }

  /** Cancels the secure session of the provided transaction, if any (rejected card). */
  private fun cancelSecureSession(cardTransaction: TransactionManager<*>) {
    if (cardTransaction is SecureRegularModeTransactionManager ||
        cardTransaction is SecurePkiModeTransactionManager) {
      cardTransaction.prepareCancelSecureSession().processCommands(ChannelControl.CLOSE_AFTER)
    }
  }

  /**
   * An event is considered valid for display if an eventTimeStamp or an eventDateStamp has been set
   * during a previous validation
   */
  private fun isValidEvent(event: EventStructure): Boolean {
    return event.eventTimeStamp.value != 0 || event.eventDateStamp.value != 0
  }
}
