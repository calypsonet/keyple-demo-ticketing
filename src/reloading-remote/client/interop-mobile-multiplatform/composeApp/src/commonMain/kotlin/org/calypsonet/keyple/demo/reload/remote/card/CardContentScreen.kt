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
package org.calypsonet.keyple.demo.reload.remote.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.basket_multi_trip
import org.calypsonet.keyple.composeapp.generated.resources.basket_season_pass
import org.calypsonet.keyple.composeapp.generated.resources.card_empty
import org.calypsonet.keyple.demo.reload.remote.AppState
import org.calypsonet.keyple.demo.reload.remote.ContractInfo
import org.calypsonet.keyple.demo.reload.remote.nav.Home
import org.calypsonet.keyple.demo.reload.remote.nav.LoadContract
import org.calypsonet.keyple.demo.reload.remote.ui.KeypleTopAppBar
import org.calypsonet.keyple.demo.reload.remote.ui.blue
import org.calypsonet.keyple.demo.reload.remote.ui.grey
import org.calypsonet.keyple.demo.reload.remote.ui.lightBlue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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

@Composable
fun CardContentScreen(
    navController: NavController,
    appState: AppState,
    viewModel: CardContentScreenViewModel,
    modifier: Modifier = Modifier
) {
  val state = viewModel.state.collectAsState()

  Scaffold(
      topBar = {
        KeypleTopAppBar(
            navController = navController,
            appState = appState,
            onBack = {
              when (state.value) {
                is CardContentScreenState.DisplayContent -> navController.navigate(Home)
                is CardContentScreenState.ChooseProduct -> viewModel.displayContent()
                is CardContentScreenState.DisplayBasket -> viewModel.chooseProduct()
              }
            })
      },
      modifier = modifier,
  ) { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = state.value.screenTitle,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
      )

      when (state.value) {
        is CardContentScreenState.DisplayContent -> {
          CardContent(
              contracts = (state.value as CardContentScreenState.DisplayContent).contracts,
              modifier = modifier,
              chooseProduct = { viewModel.chooseProduct() })
        }
        is CardContentScreenState.ChooseProduct -> {
          ProductList(
              products = (state.value as CardContentScreenState.ChooseProduct).products,
              addToBasket = viewModel::addToBasket)
        }
        is CardContentScreenState.DisplayBasket -> {
          Basket(
              product = (state.value as CardContentScreenState.DisplayBasket).selectedProduct!!,
              onPay = {
                navController.navigate(
                    LoadContract(product = it, cardSerial = viewModel.getCardSerial()))
              })
        }
      }
    }
  }
}

@Composable
internal fun ColumnScope.CardContent(
    contracts: List<ContractInfo>,
    chooseProduct: () -> Unit,
    modifier: Modifier = Modifier
) {
  if (contracts.isEmpty()) {
    Spacer(modifier = Modifier.weight(1f))
    Text(
        text = stringResource(Res.string.card_empty),
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
        color = blue,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
  }
  LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    items(contracts) { contract ->
      Card(
          modifier = Modifier.padding(16.dp).fillMaxWidth(),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
          colors = CardDefaults.cardColors(containerColor = lightBlue),
      ) {
        Column {
          Text(
              text = contract.name,
              modifier = Modifier.padding(10.dp).fillMaxWidth(),
              color = blue,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
          )

          Text(
              text = contract.description,
              modifier = Modifier.padding(10.dp).fillMaxWidth(),
              color = blue,
              textAlign = TextAlign.Center,
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.weight(1f))

  Button(
      onClick = { chooseProduct() },
      modifier = Modifier.sizeIn(maxWidth = 400.dp, minHeight = 100.dp).padding(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = blue),
      shape = RoundedCornerShape(4.dp)) {
        Text(
            "BUY TICKET",
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
        )
      }
}

@Composable
internal fun ColumnScope.Basket(product: Product, onPay: (product: Product) -> Unit) {
  ProductCard(product = product, modifier = Modifier.padding(vertical = 48.dp), onProductClick = {})

  Box(
      modifier = Modifier.fillMaxWidth().weight(1f).padding(4.dp).background(lightBlue),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CreditCardDetails()

      Button(
          onClick = { onPay(product) },
          modifier = Modifier.sizeIn(maxWidth = 400.dp, minHeight = 100.dp).padding(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = blue),
          shape = RoundedCornerShape(4.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "PAY",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
            )
          }
    }
  }
}

@Composable
internal fun CreditCardDetails() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(2.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Credit Card Details",
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
    )

    Card(
        modifier = Modifier.sizeIn(maxWidth = 400.dp, minHeight = 100.dp).padding(bottom = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth().height(100.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "Card Number", color = grey)
            Text(
                text = "1111.1111.1111.1111",
                color = blue,
            )
          }
        }

    Card(
        modifier = Modifier.sizeIn(maxWidth = 400.dp, minHeight = 100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(4.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth().height(100.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = "Expiry",
            )
            Text(
                text = "10/24",
                color = blue,
            )
            Text(
                text = "CVC",
            )
            Text(
                text = "123",
                color = blue,
            )
          }
        }
  }
}

@Composable
internal fun ProductList(
    products: List<Product>,
    modifier: Modifier = Modifier,
    addToBasket: (Product) -> Unit
) {
  LazyColumn(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        items(products) { product -> ProductCard(product = product, onProductClick = addToBasket) }
      }
}

@Composable
internal fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onProductClick: (product: Product) -> Unit
) {
  Card(
      modifier =
          modifier.padding(16.dp).sizeIn(maxWidth = 300.dp, minHeight = 100.dp).clickable {
            onProductClick(product)
          },
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
      colors = CardDefaults.cardColors(containerColor = lightBlue),
      shape = RoundedCornerShape(4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
          Text(
              text = getProductDisplayName(product),
              color = blue,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
          )
          Text(
              text = "${product.price},00 €",
              color = blue,
              textAlign = TextAlign.Center,
          )
        }
      }
}

@Composable
fun getProductDisplayName(product: Product): String {
  if (product.type == ProductType.MULTI_TRIP) {
    return pluralStringResource(Res.plurals.basket_multi_trip, product.quantity, product.quantity)
  }
  return stringResource(Res.string.basket_season_pass)
}
