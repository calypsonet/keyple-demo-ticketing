/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.dto

/**
 * - applicationSerialNumber: Serial number of the card application, in hexadecimal (empty if the
 *   card could not be selected), sent back by the client to load the trips in the same card.
 * - items: Contracts present in the card, formatted as text.
 * - statusCode: code of the [RemoteServiceStatus]: 0 (successful), 1 (card communication error), 2
 *   (server error), 4 (card not personalized), 5 (environment expired).
 * - message: Status message.
 */
data class SelectAppAndReadContractsOutputDto(
    var applicationSerialNumber: String,
    var items: List<String>,
    var statusCode: Int,
    var message: String
)
