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
package org.calypsonet.keyple.demo.reload.remote.ui.model

import kotlinx.serialization.Serializable

/** Product proposed for sale: type, price and quantity (number of trips). */
@Serializable
enum class ProductType {
  MULTI_TRIP,
  SEASON_PASS
}

@Serializable
data class Product(
    val type: ProductType,
    val price: Int,
    val quantity: Int = 1,
    val date: String? = null
)
