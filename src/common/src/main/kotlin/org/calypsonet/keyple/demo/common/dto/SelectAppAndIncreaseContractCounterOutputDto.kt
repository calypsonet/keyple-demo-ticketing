/* ******************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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
 * - statusCode: code of the [RemoteServiceStatus]: 0 (successful), 1 (card communication error), 2
 *   (server error), 4 (card not personalized), 5 (expired environment).
 * - message: Status message.
 */
data class SelectAppAndIncreaseContractCounterOutputDto(var statusCode: Int, var message: String)
