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
package org.calypsonet.keyple.demo.inspection.domain.usecases

import org.calypsonet.keyple.demo.common.reference.LocationRepository
import org.calypsonet.keyple.demo.inspection.domain.CardReaderSession
import org.calypsonet.keyple.demo.inspection.domain.CardReaderSession.CardSelection
import org.calypsonet.keyple.demo.inspection.domain.model.InspectionResult
import org.calypsonet.keyple.demo.inspection.domain.model.TechnicalError
import org.calypsonet.keyple.demo.inspection.domain.procedures.InspectionContext
import org.calypsonet.keyple.demo.inspection.domain.procedures.InspectionProcedure
import org.calypsonet.keyple.demo.inspection.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.inspection.domain.spi.Clock
import org.calypsonet.keyple.demo.inspection.domain.spi.Logger
import org.calypsonet.keyple.demo.inspection.domain.spi.UserFeedback
import org.eclipse.keypop.reader.selection.ScheduledCardSelectionsResponse

/**
 * Inspection of a presented card: selection of the card, execution of the inspection procedure
 * supporting it, and user feedback (sound) of the result.
 */
class InspectCard(
    private val cardReaderSession: CardReaderSession,
    private val appSettings: AppSettingsRepository,
    private val clock: Clock,
    private val userFeedback: UserFeedback,
    private val logger: Logger,
    private val inspectionProcedures: List<InspectionProcedure>
) {

  /** Inspects the card selected by the provided selection scenario response. */
  operator fun invoke(selectionResponse: ScheduledCardSelectionsResponse): InspectionResult {
    val result = inspect(selectionResponse)
    log(result)
    if (result is InspectionResult.CardContent) {
      userFeedback.signalSuccess()
    } else {
      userFeedback.signalFailure()
    }
    return result
  }

  private fun inspect(selectionResponse: ScheduledCardSelectionsResponse): InspectionResult {
    val card =
        when (val selection = cardReaderSession.selectCard(selectionResponse)) {
          is CardSelection.Selected -> selection.card
          is CardSelection.NotSelected ->
              return InspectionResult.NotRecognized(selection.error, selection.fileStructure)
        }
    return try {
      val procedure =
          inspectionProcedures.firstOrNull { it.supports(card) }
              ?: error("Unsupported card type [type=${card::class.simpleName}]")
      procedure.execute(
          InspectionContext(
              cardReader = cardReaderSession.cardReader,
              card = card,
              dateTime = clock.now(),
              location = appSettings.location,
              locations = LocationRepository.getLocations(),
              validationPeriodMinutes = appSettings.validationPeriodMinutes,
              symmetricCryptoSecuritySetting = cardReaderSession.symmetricCryptoSecuritySetting,
              asymmetricCryptoSecuritySetting = cardReaderSession.asymmetricCryptoSecuritySetting))
    } catch (e: Exception) {
      logger.e("Error while inspecting the card", e)
      InspectionResult.Failed(TechnicalError.UNEXPECTED)
    }
  }

  /** Logs the result of the inspection, the technical errors being logged where they occur. */
  private fun log(result: InspectionResult) {
    when (result) {
      is InspectionResult.CardContent ->
          logger.i(
              "Card inspected [authenticationMode=${result.authenticationMode}, " +
                  "contracts=${result.contracts.size}, " +
                  "validatedContract=${result.contracts.any { it.isValidated }}]")
      InspectionResult.EmptyCard -> logger.i("Card inspected: card never loaded")
      is InspectionResult.Rejected -> logger.w("Card rejected [reason=${result.reason}]")
      is InspectionResult.NotRecognized ->
          logger.w(
              "Card not recognized [error=${result.error}, fileStructure=${result.fileStructure}]")
      is InspectionResult.Failed -> {}
    }
  }
}
