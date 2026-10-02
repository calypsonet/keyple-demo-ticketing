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

import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult

fun ControlResult.toUi(): UiControlResult =
    UiControlResult(
        status = status,
        authenticationMode = authenticationMode,
        lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toUi() },
        titlesList = titlesList.mapTo(ArrayList()) { it.toUi() },
        errorTitle = errorTitle,
        errorMessage = errorMessage)

fun UiControlResult.toDomain(): ControlResult =
    ControlResult(
        status = status,
        authenticationMode = authenticationMode,
        lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toDomain() },
        titlesList = titlesList.mapTo(ArrayList()) { it.toDomain() },
        errorTitle = errorTitle,
        errorMessage = errorMessage,
    )
