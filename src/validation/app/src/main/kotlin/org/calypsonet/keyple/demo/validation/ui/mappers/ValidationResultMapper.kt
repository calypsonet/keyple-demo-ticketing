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
package org.calypsonet.keyple.demo.validation.ui.mappers

import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.ui.model.UIValidationResult

fun ValidationResult.toUi(): UIValidationResult =
    UIValidationResult(
        status = status,
        cardType = cardType,
        nbTicketsLeft = nbTicketsLeft,
        contract = contract,
        validationData = validationData?.toUi(),
        eventDateTime = eventDateTime,
        passValidityEndDate = passValidityEndDate,
        errorMessage = errorMessage)

fun UIValidationResult.toDomain(): ValidationResult =
    ValidationResult(
        status = status,
        cardType = cardType,
        nbTicketsLeft = nbTicketsLeft,
        contract = contract,
        validationData = validationData?.toDomain(),
        eventDateTime = eventDateTime,
        passValidityEndDate = passValidityEndDate,
        errorMessage = errorMessage)
