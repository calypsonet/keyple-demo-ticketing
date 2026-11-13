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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.ic_load_tickets
import org.calypsonet.keyple.composeapp.generated.resources.ic_logo_calypso
import org.calypsonet.keyple.composeapp.generated.resources.ic_personalize_card
import org.calypsonet.keyple.composeapp.generated.resources.ic_settings
import org.calypsonet.keyple.demo.sales.APP_VERSION
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.AppTopBar
import org.calypsonet.keyple.demo.sales.ui.components.KeypleBackground
import org.calypsonet.keyple.demo.sales.ui.components.TOOLBAR_ICON_SIZE
import org.calypsonet.keyple.demo.sales.ui.nav.PersonalizeCard
import org.calypsonet.keyple.demo.sales.ui.nav.ReadCard
import org.calypsonet.keyple.demo.sales.ui.nav.ServerSettings
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.LocalAppDimens
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.white
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

/** Text size of the version, as in the other applications of the demo. */
private val VERSION_TEXT_SIZE = 11.sp

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier, appState: AppState) {
  val dimens = LocalAppDimens.current
  Scaffold(
      modifier = modifier,
  ) { innerPadding ->
    // As in the Android client, the background extends behind the system bars
    KeypleBackground {
      // The insets applied here are consumed, so that the top bar does not apply them again
      Box(
          modifier =
              Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).fillMaxSize()) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              AppTopBar(
                  navController = navController,
                  appState = appState,
                  showBackArrow = false,
                  actions = {
                    // The settings button opens the server settings, the only settings of the
                    // application
                    IconButton(onClick = { navController.navigate(route = ServerSettings) }) {
                      Icon(
                          imageVector = vectorResource(Res.drawable.ic_settings),
                          contentDescription = "Go to settings action",
                          modifier = Modifier.size(TOOLBAR_ICON_SIZE),
                          tint = Color.White)
                    }
                  })

              Column(
                  modifier = modifier.padding(horizontal = 38.dp).sizeIn(maxWidth = 400.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                    text = "Open Source API for Smart Ticketing",
                    modifier = Modifier.width(dimens.homeTextWidth),
                    style = AppTextStyles.mediumRegular,
                    color = blue,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Main operation first: loading of tickets, then personalization of a card (reset
                // of
                // its data)
                HomeCard(
                    iconRes = Res.drawable.ic_load_tickets,
                    iconSize = dimens.homeIconSize,
                    height = dimens.homeCardHeight,
                    title = "Load tickets",
                    onClick = { navController.navigate(route = ReadCard) })

                Spacer(modifier = Modifier.height(dimens.homeCardGap))

                HomeCard(
                    iconRes = Res.drawable.ic_personalize_card,
                    iconSize = dimens.homeIconSize,
                    height = dimens.homeCardHeight,
                    title = "Personalize a card",
                    onClick = { navController.navigate(route = PersonalizeCard) })

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    imageVector = vectorResource(Res.drawable.ic_logo_calypso),
                    contentDescription = "Calypso logo",
                    modifier = Modifier.width(dimens.calypsoLogoWidth).padding(bottom = 4.dp))
              }
            }

            // Version of the application, in the bottom right corner as in the other applications
            // of the demo, below the hint distinguishing this client from the native Android client
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                horizontalAlignment = Alignment.End) {
                  Text(text = "Interop client", fontSize = VERSION_TEXT_SIZE, color = darkBlue)
                  Text(
                      text = "version $APP_VERSION", fontSize = VERSION_TEXT_SIZE, color = darkBlue)
                }
          }
    }
  }
}

@Composable
internal fun HomeCard(
    modifier: Modifier = Modifier,
    iconRes: DrawableResource,
    iconSize: Dp,
    height: Dp,
    title: String,
    onClick: () -> Unit
) {
  Card(
      modifier = modifier.fillMaxWidth().height(height).clickable { onClick() },
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
          style = AppTextStyles.bigBold,
          color = darkBlue)
    }
  }
}
