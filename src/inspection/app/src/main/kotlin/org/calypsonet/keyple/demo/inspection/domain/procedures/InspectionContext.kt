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
package org.calypsonet.keyple.demo.inspection.domain.procedures

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.reference.Location
import org.eclipse.keypop.calypso.card.transaction.AsymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Data of an inspection transaction, provided to the [InspectionProcedure].
 *
 * @property cardReader The reader of the card.
 * @property card The selected card.
 * @property dateTime The date and time of the inspection.
 * @property location The location of the inspection.
 * @property locations The known locations, to display those of the validations.
 * @property validationPeriodMinutes The validity period of a validation, in minutes.
 * @property symmetricCryptoSecuritySetting The security setting of the Calypso secure sessions,
 *   null if no SAM is available.
 * @property asymmetricCryptoSecuritySetting The security setting of the Calypso PKI transactions.
 */
data class InspectionContext(
    val cardReader: CardReader,
    val card: SmartCard,
    val dateTime: LocalDateTime,
    val location: Location,
    val locations: List<Location>,
    val validationPeriodMinutes: Int,
    val symmetricCryptoSecuritySetting: SymmetricCryptoSecuritySetting?,
    val asymmetricCryptoSecuritySetting: AsymmetricCryptoSecuritySetting
)
