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
package org.calypsonet.keyple.demo.reload.remote.domain.model

/** Information about the card presented to the terminal. */
data class CardInfo(
    /** Description of the card type (e.g. "CALYPSO: DF name ..." or the storage card type). */
    val description: String,
    /** Application serial number of a Calypso card, or UID of a storage card (hexadecimal). */
    val serialNumber: String,
    /** True for a storage card, false for a Calypso card. */
    val isStorageCard: Boolean,
    /** Application subtype of a Calypso card (hexadecimal), null for a storage card. */
    val applicationSubtype: String? = null
)
