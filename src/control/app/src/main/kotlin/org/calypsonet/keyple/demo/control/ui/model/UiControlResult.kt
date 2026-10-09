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
package org.calypsonet.keyple.demo.control.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Control result displayed by the UI, passed to the screen displaying it. */
@Parcelize
data class UiControlResult(
    val status: Status,
    val lastValidationsList: List<UiValidation>? = null,
    val contractsList: List<UiContract>,
    val errorMessage: String? = null
) : Parcelable
