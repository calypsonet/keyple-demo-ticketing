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
package org.calypsonet.keyple.demo.control.domain.procedures

import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.Location
import org.eclipse.keypop.calypso.card.transaction.AsymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Data of a control transaction, provided to the [ControlProcedure].
 *
 * @property cardReader The reader of the card.
 * @property card The selected card.
 * @property dateTime The date and time of the control.
 * @property location The location of the control.
 * @property locations The known locations, to display those of the validations.
 * @property validationPeriod The validity period of a validation, in minutes.
 * @property symmetricCryptoSecuritySetting The security setting of the Calypso secure sessions,
 *   null if no SAM is available.
 * @property asymmetricCryptoSecuritySetting The security setting of the Calypso PKI transactions.
 */
data class ControlContext(
    val cardReader: CardReader,
    val card: SmartCard,
    val dateTime: LocalDateTime,
    val location: Location,
    val locations: List<Location>,
    val validationPeriod: Int,
    val symmetricCryptoSecuritySetting: SymmetricCryptoSecuritySetting?,
    val asymmetricCryptoSecuritySetting: AsymmetricCryptoSecuritySetting
)
