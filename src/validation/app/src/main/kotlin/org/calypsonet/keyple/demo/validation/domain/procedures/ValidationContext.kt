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
package org.calypsonet.keyple.demo.validation.domain.procedures

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.Location
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Data of a validation transaction, provided to the [ValidationProcedure].
 *
 * @property cardReader The reader of the card.
 * @property card The selected card.
 * @property dateTime The date and time of the validation.
 * @property location The location of the validation.
 * @property locations The known locations, to build the validation data.
 * @property cardSecuritySetting The security setting of the Calypso secure sessions, null if no SAM
 *   is available.
 */
data class ValidationContext(
    val cardReader: CardReader,
    val card: SmartCard,
    val dateTime: LocalDateTime,
    val location: Location,
    val locations: List<Location>,
    val cardSecuritySetting: SymmetricCryptoSecuritySetting?
)
