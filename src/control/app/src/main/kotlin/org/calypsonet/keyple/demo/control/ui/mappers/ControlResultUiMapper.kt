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
package org.calypsonet.keyple.demo.control.ui.mappers

import android.content.res.Resources
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.domain.model.RejectionReason
import org.calypsonet.keyple.demo.control.domain.model.TechnicalError
import org.calypsonet.keyple.demo.control.ui.model.Status
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult

/** Translates the control result into the result displayed, with its texts. */
fun ControlResult.toUi(resources: Resources): UiControlResult =
    when (this) {
      is ControlResult.CardContent ->
          UiControlResult(
              status = Status.TICKETS_FOUND,
              lastValidationsList = lastValidation?.let { listOf(it.toUi()) },
              contractsList = contracts.map { it.toUi() })
      ControlResult.EmptyCard ->
          UiControlResult(status = Status.EMPTY_CARD, contractsList = emptyList())
      is ControlResult.Rejected ->
          UiControlResult(
              status = Status.ERROR,
              contractsList = emptyList(),
              errorMessage = resources.getString(reason.message()))
      is ControlResult.Failed ->
          UiControlResult(
              status = Status.ERROR,
              contractsList = emptyList(),
              errorMessage =
                  when (error) {
                    TechnicalError.MIFARE_CLASSIC_AUTHENTICATION_FAILED ->
                        resources.getString(R.string.error_mifare_classic_authentication)
                    TechnicalError.MIFARE_CLASSIC_READING_FAILED ->
                        resources.getString(R.string.error_mifare_classic_reading)
                    TechnicalError.UNEXPECTED ->
                        detail ?: resources.getString(R.string.error_card_reading)
                  })
    }

/** Returns the message displayed for the rejection reason. */
private fun RejectionReason.message(): Int =
    when (this) {
      RejectionReason.ENVIRONMENT_WRONG_VERSION -> R.string.rejection_environment_wrong_version
      RejectionReason.ENVIRONMENT_EXPIRED -> R.string.rejection_environment_expired
      RejectionReason.EVENT_WRONG_VERSION -> R.string.rejection_event_wrong_version
      RejectionReason.CONTRACT_WRONG_VERSION -> R.string.rejection_contract_wrong_version
    }
