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
package org.calypsonet.keyple.demo.reload.remote.nav

import kotlinx.serialization.Serializable
import org.calypsonet.keyple.demo.reload.remote.card.Product

@Serializable data object Home

@Serializable data object Settings

enum class ScanNavArgs(val value: String) {
  READ_CONTRACTS("read-contracts"),
  PERSONALIZE_CARD("personalize-card"),
  LOAD_CONTRACT("load-contract")
}

@Serializable data class Scan(val action: String = ScanNavArgs.READ_CONTRACTS.value)

@Serializable
data class LoadContract(
    val type: Int,
    val price: Int,
    val quantity: Int = 1,
    val date: String? = null,
    val cardSerial: String
) {
  companion object {
    operator fun invoke(product: Product, cardSerial: String = ""): LoadContract {
      return LoadContract(
          product.type.ordinal, product.price, product.quantity, product.date, cardSerial)
    }
  }
}

@Serializable data object PersonalizeCard

@Serializable data object ReadCard

@Serializable data object Card

@Serializable data object AppSuccess

@Serializable data object ServerConfig
