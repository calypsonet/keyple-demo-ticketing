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
package org.calypsonet.keyple.demo.validation.domain.usecases

import org.calypsonet.keyple.demo.validation.domain.CardReaderSession
import org.calypsonet.keyple.demo.validation.domain.CardReaderSession.CardSelection
import org.calypsonet.keyple.demo.validation.domain.model.TechnicalError
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.procedures.ValidationContext
import org.calypsonet.keyple.demo.validation.domain.procedures.ValidationProcedure
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.validation.domain.spi.Clock
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.UserFeedback
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse

/**
 * Validation of a presented card: selection of the card, execution of the validation procedure
 * supporting it, and user feedback (sound, LEDs) of the result.
 */
class ValidateCard(
    private val cardReaderSession: CardReaderSession,
    private val appSettings: AppSettingsRepository,
    private val clock: Clock,
    private val userFeedback: UserFeedback,
    private val logger: Logger,
    private val validationProcedures: List<ValidationProcedure>
) {

  /** Validates the card selected by the provided selection scenario response. */
  operator fun invoke(selectionResponse: ScheduledCardSelectionsResponse): ValidationResult {
    val result = validate(selectionResponse)
    log(result)
    when (result) {
      is ValidationResult.Accepted -> userFeedback.signalSuccess()
      is ValidationResult.CardRemoved -> {
        // Card removed during the transaction: no feedback
      }
      else -> userFeedback.signalFailure()
    }
    return result
  }

  private fun validate(selectionResponse: ScheduledCardSelectionsResponse): ValidationResult {
    val card =
        when (val selection = cardReaderSession.selectCard(selectionResponse)) {
          is CardSelection.Selected -> selection.card
          is CardSelection.NotSelected ->
              return ValidationResult.NotRecognized(selection.error, selection.fileStructure)
        }
    return try {
      val procedure =
          validationProcedures.firstOrNull { it.supports(card) }
              ?: error("Unsupported card type [type=${card::class.simpleName}]")
      procedure.execute(
          ValidationContext(
              cardReader = cardReaderSession.cardReader,
              card = card,
              dateTime = clock.now(),
              location = appSettings.location,
              cardSecuritySetting = cardReaderSession.cardSecuritySetting))
    } catch (e: Exception) {
      logger.e("Error while validating the card", e)
      ValidationResult.Failed(cardDescription = null, error = TechnicalError.UNEXPECTED)
    }
  }

  /** Logs the result of the validation, the technical errors being logged where they occur. */
  private fun log(result: ValidationResult) {
    when (result) {
      is ValidationResult.Accepted ->
          logger.i(
              "Card validated [cardDescription=${result.cardDescription}, remainingTrips=${result.remainingTrips}, " +
                  "seasonPassValidityEndDate=${result.seasonPassValidityEndDate}]")
      is ValidationResult.Rejected ->
          logger.w(
              "Card rejected [cardDescription=${result.cardDescription}, reason=${result.reason}]")
      is ValidationResult.CardRemoved ->
          logger.w(
              "Card removed during the transaction [cardDescription=${result.cardDescription}]")
      is ValidationResult.NotRecognized ->
          logger.w(
              "Card not recognized [error=${result.error}, fileStructure=${result.fileStructure}]")
      is ValidationResult.Failed -> {}
    }
  }
}
