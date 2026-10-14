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
package org.calypsonet.keyple.demo.control.domain.usecases

import org.calypsonet.keyple.demo.common.reference.LocationRepository
import org.calypsonet.keyple.demo.control.domain.CardReaderSession
import org.calypsonet.keyple.demo.control.domain.CardReaderSession.CardSelection
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.model.TechnicalError
import org.calypsonet.keyple.demo.control.domain.procedures.ControlContext
import org.calypsonet.keyple.demo.control.domain.procedures.ControlProcedure
import org.calypsonet.keyple.demo.control.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.control.domain.spi.Clock
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.domain.spi.UserFeedback
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse

/**
 * Control of a presented card: selection of the card, execution of the control procedure supporting
 * it, and user feedback (sound) of the result.
 */
class ControlCard(
    private val cardReaderSession: CardReaderSession,
    private val appSettings: AppSettingsRepository,
    private val clock: Clock,
    private val userFeedback: UserFeedback,
    private val logger: Logger,
    private val controlProcedures: List<ControlProcedure>
) {

  /** Controls the card selected by the provided selection scenario response. */
  operator fun invoke(selectionResponse: ScheduledCardSelectionsResponse): ControlResult {
    val result = control(selectionResponse)
    log(result)
    if (result is ControlResult.CardContent) {
      userFeedback.displayResultSuccess()
    } else {
      userFeedback.displayResultFailed()
    }
    return result
  }

  private fun control(selectionResponse: ScheduledCardSelectionsResponse): ControlResult {
    val card =
        when (val selection = cardReaderSession.selectCard(selectionResponse)) {
          is CardSelection.Selected -> selection.card
          is CardSelection.NotSelected ->
              return ControlResult.NotSelected(selection.error, selection.fileStructure)
        }
    return try {
      val procedure =
          controlProcedures.firstOrNull { it.supports(card) }
              ?: error("Unsupported card type: ${card::class.simpleName}")
      procedure.execute(
          ControlContext(
              cardReader = cardReaderSession.cardReader,
              card = card,
              dateTime = clock.now(),
              location = appSettings.location,
              locations = LocationRepository.getLocations(),
              validationPeriod = appSettings.validationPeriod,
              symmetricCryptoSecuritySetting = cardReaderSession.symmetricCryptoSecuritySetting,
              asymmetricCryptoSecuritySetting = cardReaderSession.asymmetricCryptoSecuritySetting))
    } catch (e: Exception) {
      logger.e("Error while controlling the card", e)
      ControlResult.Failed(TechnicalError.UNEXPECTED, e.message)
    }
  }

  /** Logs the result of the control, the technical errors being logged where they occur. */
  private fun log(result: ControlResult) {
    when (result) {
      is ControlResult.CardContent ->
          logger.i(
              "Card controlled [authenticationMode=${result.authenticationMode}, " +
                  "contracts=${result.contracts.size}, " +
                  "validatedContract=${result.contracts.any { it.isValidated }}]")
      ControlResult.EmptyCard -> logger.i("Card controlled: card never loaded")
      is ControlResult.Rejected -> logger.w("Card refused [reason=${result.reason}]")
      is ControlResult.NotSelected ->
          logger.w(
              "Card not selected [error=${result.error}, fileStructure=${result.fileStructure}]")
      is ControlResult.Failed -> {}
    }
  }
}
