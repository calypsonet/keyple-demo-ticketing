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
package org.calypsonet.keyple.demo.validation.ui.model

import android.os.Parcelable
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.parcelize.Parcelize
import org.calypsonet.keyple.demo.validation.domain.model.Status

@Parcelize
data class UiValidationResult(
    val status: Status,
    val cardType: String,
    val nbTicketsLeft: Int? = null,
    val contract: String?,
    val validationData: UiValidationData?,
    val eventDateTime: LocalDateTime? = null,
    val passValidityEndDate: LocalDate? = null,
    val errorMessage: String? = null
) : Parcelable
