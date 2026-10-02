/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.calypsonet.keyple.demo.reload.remote.domain.model.Status

@Parcelize
data class UiCardReaderResponse(
    val status: Status,
    val cardType: String,
    val ticketsNumber: Int,
    val titlesList: List<UiCardTitle>,
    val lastValidationsList: ArrayList<UiValidation>,
    val seasonPassExpiryDate: String,
    val errorMessage: String? = null
) : Parcelable
