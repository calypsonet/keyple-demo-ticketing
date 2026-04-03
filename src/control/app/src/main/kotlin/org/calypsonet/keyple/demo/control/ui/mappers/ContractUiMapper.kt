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

import org.calypsonet.keyple.demo.control.domain.model.Contract
import org.calypsonet.keyple.demo.control.ui.model.UiContract

fun Contract.toUi(): UiContract =
    UiContract(
        name = name,
        valid = valid,
        validationDateTime = validationDateTime,
        record = record,
        expired = expired,
        contractValidityStartDate = contractValidityStartDate,
        contractValidityEndDate = contractValidityEndDate,
        nbTicketsLeft = nbTicketsLeft)

fun UiContract.toDomain(): Contract =
    Contract(
        name = name,
        valid = valid,
        validationDateTime = validationDateTime,
        record = record,
        expired = expired,
        contractValidityStartDate = contractValidityStartDate,
        contractValidityEndDate = contractValidityEndDate,
        nbTicketsLeft = nbTicketsLeft)
