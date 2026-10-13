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
package org.calypsonet.keyple.demo.reload.remote.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.calypsonet.keyple.demo.reload.remote.data.dto.SelectAppAndAnalyzeContractsOutputDto.ContractInfo
import org.calypsonet.keyple.demo.reload.remote.data.service.ReadCardCache
import org.calypsonet.keyple.demo.reload.remote.ui.model.Product
import org.calypsonet.keyple.demo.reload.remote.ui.model.ProductType

sealed class CardContentScreenState(val screenTitle: String) {
  data class DisplayContent(val contracts: List<ContractInfo> = emptyList()) :
      CardContentScreenState("Content")

  data class ChooseProduct(val products: List<Product> = emptyList()) :
      CardContentScreenState("Choose a type of ticket")

  data class DisplayBasket(val selectedProduct: Product?) : CardContentScreenState("Basket")
}

class CardContentScreenViewModel(private val readCardCache: ReadCardCache) : ViewModel() {

  private var _state =
      MutableStateFlow<CardContentScreenState>(
          CardContentScreenState.DisplayContent(readCardCache.getCardContracts()))

  val state =
      _state.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = CardContentScreenState.DisplayContent(readCardCache.getCardContracts()))

  fun displayContent() {
    val contracts = readCardCache.getCardContracts()
    _state.value = CardContentScreenState.DisplayContent(contracts)
  }

  fun chooseProduct() {
    _state.value =
        CardContentScreenState.ChooseProduct(
            listOf(
                Product(type = ProductType.MULTI_TRIP, price = 1, quantity = 1),
                Product(type = ProductType.MULTI_TRIP, price = 2, quantity = 2),
                Product(type = ProductType.MULTI_TRIP, price = 3, quantity = 3),
                Product(type = ProductType.MULTI_TRIP, price = 4, quantity = 4),
                Product(
                    type = ProductType.SEASON_PASS, price = 20, quantity = 1, date = "12/12/24"),
            ))
  }

  fun addToBasket(product: Product) {
    _state.value = CardContentScreenState.DisplayBasket(product)
  }

  fun getCardSerial(): String {
    return readCardCache.getCardSerial()
  }
}
