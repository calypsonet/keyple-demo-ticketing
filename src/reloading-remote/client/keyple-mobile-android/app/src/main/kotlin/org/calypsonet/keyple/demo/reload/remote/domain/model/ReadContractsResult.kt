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

import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus

/** Result of the reading of the contracts of a card by the server. */
data class ReadContractsResult(
    val card: CardInfo,
    val status: RemoteServiceStatus,
    /** Contracts of the card, empty if the status is not [RemoteServiceStatus.SUCCESS]. */
    val titles: List<CardTitle>
)
