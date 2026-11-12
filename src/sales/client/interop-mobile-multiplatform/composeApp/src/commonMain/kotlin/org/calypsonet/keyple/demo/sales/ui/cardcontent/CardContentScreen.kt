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
package org.calypsonet.keyple.demo.sales.ui.cardcontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.basket_multi_trip
import org.calypsonet.keyple.composeapp.generated.resources.basket_season_pass
import org.calypsonet.keyple.composeapp.generated.resources.ic_cvc_help
import org.calypsonet.keyple.composeapp.generated.resources.no_contract_label
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.data.dto.SelectAppAndAnalyzeContractsOutputDto.ContractInfo
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar
import org.calypsonet.keyple.demo.sales.ui.components.MainButton
import org.calypsonet.keyple.demo.sales.ui.model.Product
import org.calypsonet.keyple.demo.sales.ui.model.ProductType
import org.calypsonet.keyple.demo.sales.ui.nav.Home
import org.calypsonet.keyple.demo.sales.ui.nav.PaymentConfirmed
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.LocalAppDimens
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.grey
import org.calypsonet.keyple.demo.sales.ui.theme.lightBlue
import org.calypsonet.keyple.demo.sales.ui.theme.white
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

// Dimensions of the Android client (dimens.xml)
private val BLOCK_SPACE = 10.dp // block_space
private val ITEM_HEIGHT = 90.dp // product_button_height, contract_item_height
private val ITEM_HORIZONTAL_MARGIN = 42.dp // content_title_padding
private val ITEM_GAP = 20.dp // contract_item_margin_bottom
private val ITEM_ELEVATION = 4.dp // elevation
private val TITLE_BOTTOM_MARGIN = 20.dp // content_top_margin
private val CHOOSE_HORIZONTAL_MARGIN = 38.dp // home_horizontal_margin
private val CHOOSE_TOP_MARGIN = 7.dp // home_choose_margin_top
private val LINE_HORIZONTAL_MARGIN = 13.dp // card_validation_horizontal_margin
private val CC_PADDING = 30.dp // cc_padding
private val CC_HELP_SIZE = 17.dp // cc_help_size

// Smaller paddings of the credit card details, used when the screen is too low (vertical paddings)
// or too narrow (horizontal paddings) for those of the Android client: the basket then fits
// without its elements overlapping
private val CC_PADDING_COMPACT = 12.dp
private val CC_PADDING_NARROW = 16.dp
private val BASKET_COMPACT_HEIGHT = 560.dp
private val BASKET_NARROW_WIDTH = 400.dp

private val ITEM_SHAPE = RoundedCornerShape(5.dp)

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
        AppTopBar(
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
      when (val current = state.value) {
        is CardContentScreenState.DisplayContent -> {
          ScreenTitle(current.screenTitle)
          CardContent(
              contracts = current.contracts, onChooseProduct = { viewModel.chooseProduct() })
        }
        is CardContentScreenState.ChooseProduct -> {
          // As in the Android client, the title of the choice is a discreet text on the left
          Text(
              text = current.screenTitle,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          start = BLOCK_SPACE + CHOOSE_HORIZONTAL_MARGIN,
                          end = BLOCK_SPACE + CHOOSE_HORIZONTAL_MARGIN,
                          top = CHOOSE_TOP_MARGIN),
              style = AppTextStyles.smallRegular,
              color = darkBlue,
          )
          ProductList(
              products = current.products,
              onAddToBasket = viewModel::addToBasket,
              modifier = Modifier.weight(1f))
        }
        is CardContentScreenState.DisplayBasket -> {
          ScreenTitle(current.screenTitle)
          Basket(
              product = current.selectedProduct!!,
              // As in the Android client, the payment is confirmed before the loading
              onPay = {
                navController.navigate(
                    PaymentConfirmed(
                        product = it, cardSerialNumber = viewModel.getCardSerialNumber()))
              },
              modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

/** Title of the content and of the basket, as in the Android client (`bigBoldBlack`). */
@Composable
private fun ScreenTitle(title: String) {
  Text(
      text = title,
      modifier = Modifier.fillMaxWidth().padding(bottom = TITLE_BOTTOM_MARGIN),
      style = AppTextStyles.bigBold,
      color = Color.Black,
      textAlign = TextAlign.Center,
  )
}

/**
 * Contracts of the card, as in the Android client (`contract_recycler_row`), and the button to buy
 * a ticket, on a light blue band at the bottom of the screen.
 */
@Composable
internal fun ColumnScope.CardContent(contracts: List<ContractInfo>, onChooseProduct: () -> Unit) {
  if (contracts.isEmpty()) {
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
      Text(
          text = stringResource(Res.string.no_contract_label),
          style = AppTextStyles.bigBold,
          color = blue,
          textAlign = TextAlign.Center,
      )
    }
  } else {
    LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
          items(contracts) { contract -> ContractItem(contract) }
        }
  }

  Column(modifier = Modifier.fillMaxWidth().background(lightBlue)) {
    Box(
        modifier =
            Modifier.padding(horizontal = LINE_HORIZONTAL_MARGIN)
                .fillMaxWidth()
                .height(1.dp)
                .background(blue))
    Spacer(modifier = Modifier.height(BLOCK_SPACE))
    MainButton(text = "Buy ticket", onClick = onChooseProduct)
  }
}

@Composable
private fun ContractItem(contract: ContractInfo) {
  val textWidth = LocalAppDimens.current.contractTextWidth
  Card(
      modifier =
          Modifier.padding(
                  start = ITEM_HORIZONTAL_MARGIN, end = ITEM_HORIZONTAL_MARGIN, bottom = ITEM_GAP)
              .fillMaxWidth()
              .heightIn(min = ITEM_HEIGHT),
      shape = ITEM_SHAPE,
      colors = CardDefaults.cardColors(containerColor = lightBlue),
      elevation = CardDefaults.cardElevation(defaultElevation = ITEM_ELEVATION),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = ITEM_HEIGHT).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      Text(
          text = contract.name,
          modifier = Modifier.width(textWidth),
          style = AppTextStyles.smallBold,
          color = blue,
          textAlign = TextAlign.Center,
      )
      Text(
          text = contract.description,
          modifier = Modifier.width(textWidth),
          style = AppTextStyles.smallerRegular,
          color = blue,
          textAlign = TextAlign.Center,
      )
    }
  }
}

/**
 * Column filling the height available, which scrolls when its content does not fit (small screens,
 * large fonts): its weighted children share the space left only when the content fits.
 */
@Composable
private fun FillOrScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(maxWidth: Dp, maxHeight: Dp) -> Unit
) {
  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val width = maxWidth
    val height = maxHeight
    Column(
        modifier =
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = height),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      content(width, height)
    }
  }
}

/**
 * Products proposed for sale, as in the Android client (`activity_select_product`): spread over the
 * height of the screen, or scrolling when they do not fit.
 */
@Composable
internal fun ProductList(
    products: List<Product>,
    modifier: Modifier = Modifier,
    onAddToBasket: (Product) -> Unit
) {
  FillOrScrollColumn(modifier = modifier.padding(horizontal = BLOCK_SPACE)) { _, _ ->
    products.forEach { product ->
      Spacer(modifier = Modifier.weight(1f))
      Spacer(modifier = Modifier.height(8.dp))
      ProductCard(product = product, onProductClick = onAddToBasket)
    }
    Spacer(modifier = Modifier.weight(1f))
    Spacer(modifier = Modifier.height(8.dp))
  }
}

/**
 * Basket, as in the Android client (`activity_checkout`): the product chosen, then the credit card
 * details and the Pay button at the bottom of the screen, on a light blue panel.
 */
@Composable
internal fun Basket(product: Product, onPay: (product: Product) -> Unit, modifier: Modifier) {
  FillOrScrollColumn(modifier = modifier) { maxWidth, maxHeight ->
    Spacer(modifier = Modifier.weight(1f))
    ProductCard(product = product)
    Spacer(modifier = Modifier.weight(1f))
    Spacer(modifier = Modifier.height(16.dp))

    Column(
        modifier = Modifier.fillMaxWidth().background(lightBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CreditCardDetails(
          verticalPadding =
              if (maxHeight < BASKET_COMPACT_HEIGHT) CC_PADDING_COMPACT else CC_PADDING,
          horizontalPadding = if (maxWidth < BASKET_NARROW_WIDTH) CC_PADDING_NARROW else CC_PADDING)
      MainButton(text = "Pay", onClick = { onPay(product) })
    }
  }
}

@Composable
internal fun CreditCardDetails(verticalPadding: Dp, horizontalPadding: Dp) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = BLOCK_SPACE),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Credit card details",
        modifier = Modifier.padding(top = verticalPadding),
        style = AppTextStyles.smallBold,
        color = darkBlue,
    )

    Column(
        modifier =
            Modifier.padding(vertical = verticalPadding)
                .fillMaxWidth()
                .background(white, ITEM_SHAPE)) {
          val lineModifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = horizontalPadding, vertical = verticalPadding)
          Row(
              modifier = lineModifier,
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(text = "Card number", style = AppTextStyles.smallRegular, color = grey)
            Text(
                text = "1111 1111 1111 1111",
                modifier = Modifier.padding(start = 8.dp),
                style = AppTextStyles.smallMedium,
                color = darkBlue,
                textAlign = TextAlign.End)
          }

          Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(lightBlue))

          Row(modifier = lineModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Expiry", style = AppTextStyles.smallRegular, color = grey)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "12/30", style = AppTextStyles.smallMedium, color = darkBlue)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "CVC", style = AppTextStyles.smallRegular, color = grey)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = "123", style = AppTextStyles.smallMedium, color = darkBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                imageVector = vectorResource(Res.drawable.ic_cvc_help),
                contentDescription = "CVC help",
                modifier = Modifier.size(CC_HELP_SIZE))
          }
        }
  }
}

/**
 * Product, as in the Android client: light blue button, name (`smallBoldDarkBlue`) and price
 * (`smallRegularDarkBlue`). The product of the basket is not clickable.
 */
@Composable
internal fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onProductClick: ((product: Product) -> Unit)? = null
) {
  Card(
      modifier =
          modifier
              .padding(horizontal = ITEM_HORIZONTAL_MARGIN)
              .fillMaxWidth()
              .heightIn(min = ITEM_HEIGHT)
              .clickable(enabled = onProductClick != null) { onProductClick?.invoke(product) },
      shape = ITEM_SHAPE,
      colors = CardDefaults.cardColors(containerColor = lightBlue),
      elevation = CardDefaults.cardElevation(defaultElevation = ITEM_ELEVATION),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = ITEM_HEIGHT).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Text(
          text = getProductDisplayName(product),
          style = AppTextStyles.smallBold,
          color = darkBlue,
          textAlign = TextAlign.Center,
      )
      Text(
          text = "€${product.price}.00",
          style = AppTextStyles.smallRegular,
          color = darkBlue,
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
