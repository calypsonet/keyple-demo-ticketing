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
package org.calypsonet.keyple.demo.reload.remote.ui.nav

import kotlinx.serialization.Serializable
import org.calypsonet.keyple.demo.reload.remote.ui.model.Product

@Serializable data object Home

@Serializable data object SettingsMenu

@Serializable
data class LoadContract(
    val type: Int,
    val price: Int,
    val quantity: Int = 1,
    val cardSerialNumber: String
) {
  companion object {
    operator fun invoke(product: Product, cardSerialNumber: String = ""): LoadContract {
      return LoadContract(product.type.ordinal, product.price, product.quantity, cardSerialNumber)
    }
  }
}

@Serializable data object PersonalizeCard

@Serializable data object ReadCard

@Serializable data object CardContent

@Serializable data object Success

@Serializable data object ServerSettings
