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
import org.calypsonet.keyple.demo.control.domain.model.SelectionError
import org.calypsonet.keyple.demo.control.domain.model.TechnicalError
import org.calypsonet.keyple.demo.control.ui.model.ResultType
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult

/** Translates the control result into the result displayed, with its texts. */
fun ControlResult.toUi(resources: Resources): UiControlResult =
    when (this) {
      is ControlResult.CardContent ->
          UiControlResult(
              resultType = ResultType.CONTRACTS_FOUND,
              lastValidationsList = lastValidation?.let { listOf(it.toUi()) },
              contractsList = contracts.map { it.toUi() })
      is ControlResult.NotSelected ->
          UiControlResult(
              resultType = ResultType.INVALID_CARD,
              contractsList = emptyList(),
              errorMessage =
                  when (error) {
                    SelectionError.CARD_NOT_RECOGNIZED ->
                        resources.getString(R.string.selection_card_not_recognized)
                    SelectionError.UNEXPECTED_DF_NAME ->
                        resources.getString(R.string.selection_unexpected_df_name)
                    SelectionError.UNSUPPORTED_FILE_STRUCTURE ->
                        resources.getString(
                            R.string.selection_unsupported_file_structure, fileStructure)
                  })
      ControlResult.EmptyCard ->
          UiControlResult(resultType = ResultType.EMPTY_CARD, contractsList = emptyList())
      is ControlResult.Rejected ->
          UiControlResult(
              resultType = ResultType.ERROR,
              contractsList = emptyList(),
              errorMessage = resources.getString(reason.message()))
      is ControlResult.Failed ->
          UiControlResult(
              resultType = ResultType.ERROR,
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
