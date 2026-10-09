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
package org.calypsonet.keyple.demo.common.model

import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/** Environment and holder record of the card (see the data model in the README). */
data class EnvironmentHolderStructure(
    val envVersionNumber: VersionNumber,
    val envApplicationNumber: Int,
    val envIssuingDate: DateCompact,
    val envEndDate: DateCompact,
    val holderCompany: Int?,
    val holderIdNumber: Int?
)
