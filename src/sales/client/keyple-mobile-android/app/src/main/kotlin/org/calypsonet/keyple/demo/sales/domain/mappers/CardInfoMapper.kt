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
package org.calypsonet.keyple.demo.sales.domain.mappers

import org.calypsonet.keyple.demo.sales.domain.model.CardInfo
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.card.StorageCard

/** Builds the information of the selected card displayed by the UI. */
fun SmartCard.toCardInfo(): CardInfo =
    when (this) {
      is CalypsoCard ->
          CardInfo(
              description = "CALYPSO: DF name " + HexUtil.toHex(dfName),
              serialNumber = HexUtil.toHex(applicationSerialNumber),
              isStorageCard = false,
              applicationSubtype = HexUtil.toHex(applicationSubtype))
      is StorageCard ->
          CardInfo(
              description = productType.name,
              serialNumber = HexUtil.toHex(uid),
              isStorageCard = true)
      else -> throw IllegalStateException("Unexpected card type")
    }
