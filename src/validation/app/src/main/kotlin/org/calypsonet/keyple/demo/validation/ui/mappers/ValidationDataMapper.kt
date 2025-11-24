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

import org.calypsonet.keyple.demo.validation.domain.model.ValidationData
import org.calypsonet.keyple.demo.validation.ui.model.UIValidationData

fun ValidationData.toUi(): UIValidationData =
    UIValidationData(
        name = name,
        location = location.toUi(),
        destination = destination,
        dateTime = dateTime,
        provider = provider)

fun UIValidationData.toDomain(): ValidationData =
    ValidationData(
        name = name,
        location = location.toDomain(),
        destination = destination,
        dateTime = dateTime,
        provider = provider)
