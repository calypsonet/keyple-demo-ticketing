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

import org.calypsonet.keyple.demo.control.domain.model.Validation
import org.calypsonet.keyple.demo.control.ui.model.UiValidation

fun Validation.toUi(): UiValidation =
    UiValidation(
        name = name,
        location = location.toUi(),
        destination = destination,
        dateTime = dateTime,
        provider = provider)

fun UiValidation.toDomain(): Validation =
    Validation(
        name = name,
        location = location.toDomain(),
        destination = destination,
        dateTime = dateTime,
        provider = provider)
