/* ******************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.dto

import kotlinx.serialization.Serializable

/**
 * Input of the service personalizing the card.
 *
 * Mirror of `org.calypsonet.keyple.demo.common.dto.SelectAppAndPersonalizeCardInputDto` (JSON API
 * of the server): the `common` library being JVM only, the DTOs are declared again for the
 * multiplatform client.
 */
@Serializable
data class SelectAppAndPersonalizeCardInputDto(val pluginType: String = "Android NFC")
