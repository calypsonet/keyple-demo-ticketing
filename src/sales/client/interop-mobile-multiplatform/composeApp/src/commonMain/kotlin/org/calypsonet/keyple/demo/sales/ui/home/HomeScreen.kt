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
package org.calypsonet.keyple.demo.sales.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.ic_load_tickets
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_calypso
import org.calypsonet.keyple.composeapp.generated.resources.ic_personalize_card
import org.calypsonet.keyple.composeapp.generated.resources.ic_settings
import org.calypsonet.keyple.composeapp.generated.resources.keyple_background
import org.calypsonet.keyple.demo.sales.APP_VERSION
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar
import org.calypsonet.keyple.demo.sales.ui.nav.PersonalizeCard
import org.calypsonet.keyple.demo.sales.ui.nav.ReadCard
import org.calypsonet.keyple.demo.sales.ui.nav.ServerSettings
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.white
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier, appState: AppState) {
  Scaffold(
      modifier = modifier,
  ) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      val dimens = homeButtonDimens(maxWidth)
      Image(
          imageVector = vectorResource(Res.drawable.keyple_background),
          contentDescription = "Keyple app background",
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.matchParentSize())

      Column(
          Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AppTopBar(
            navController = navController,
            appState = appState,
            showBackArrow = false,
            actions = {
              // The settings button opens the server settings, the only settings of the application
              IconButton(onClick = { navController.navigate(route = ServerSettings) }) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    contentDescription = "Go to settings action",
                    tint = Color.White)
              }
            })

        Column(
            modifier = modifier.padding(horizontal = 38.dp).sizeIn(maxWidth = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
              text = "Open Source API for Smart Ticketing",
              modifier = Modifier.widthIn(max = 200.dp),
              color = blue,
              textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.weight(1f))

          // Main operation first: loading of tickets, then personalization of a card (reset of its
          // data)
          HomeCard(
              iconRes = Res.drawable.ic_load_tickets,
              iconSize = dimens.iconSize,
              title = "Load tickets",
              dimens = dimens,
              onClick = { navController.navigate(route = ReadCard) })

          Spacer(modifier = Modifier.height(dimens.gap))

          HomeCard(
              iconRes = Res.drawable.ic_personalize_card,
              iconSize = dimens.iconSize,
              title = "Personalize a card",
              dimens = dimens,
              onClick = { navController.navigate(route = PersonalizeCard) })

          Spacer(modifier = Modifier.weight(1f))

          // Hint distinguishing this client from the native Android client of the demo
          Text(text = "Interop client", color = darkBlue)

          Text(text = "version $APP_VERSION", color = darkBlue)

          Image(
              imageVector = vectorResource(Res.drawable.ic_logo_calypso),
              contentDescription = "Keyple logo",
              modifier = Modifier.padding(bottom = 4.dp))
        }
      }
    }
  }
}

/**
 * Dimensions of the home buttons, the same as the native Android client (resources `values`,
 * `values-sw400dp` and `values-sw600dp` of its `activity_home` layout), depending on the width of
 * the screen.
 */
internal data class HomeButtonDimens(
    val height: Dp,
    val gap: Dp,
    val iconSize: Dp,
    val textSize: TextUnit,
)

internal fun homeButtonDimens(screenWidth: Dp): HomeButtonDimens =
    when {
      screenWidth >= 600.dp -> HomeButtonDimens(168.dp, 32.dp, 56.dp, 27.sp)
      screenWidth >= 400.dp -> HomeButtonDimens(108.dp, 32.dp, 44.dp, 22.sp)
      else -> HomeButtonDimens(84.dp, 24.dp, 36.dp, 20.sp)
    }

@Composable
internal fun HomeCard(
    modifier: Modifier = Modifier,
    iconRes: DrawableResource,
    iconSize: Dp,
    title: String,
    dimens: HomeButtonDimens,
    onClick: () -> Unit
) {
  Card(
      modifier = modifier.fillMaxWidth().height(dimens.height).clickable { onClick() },
      shape = RoundedCornerShape(5.dp),
      colors = CardDefaults.cardColors(containerColor = white),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
  ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Square icon, of the same size as in the Android client
      Image(
          imageVector = vectorResource(iconRes),
          modifier = Modifier.size(iconSize),
          contentDescription = "icon for card $title",
      )
      Text(
          text = title,
          modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
          textAlign = TextAlign.Center,
          fontSize = dimens.textSize,
          fontWeight = FontWeight.Bold,
          color = darkBlue)
    }
  }
}
