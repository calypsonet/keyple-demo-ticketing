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
package org.calypsonet.keyple.demo.sales.ui.success

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import io.github.alexzhirkevich.compottie.LottieComposition
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.contract_loaded
import org.calypsonet.keyple.demo.sales.AppState
import org.calypsonet.keyple.demo.sales.ui.components.ResultKind
import org.calypsonet.keyple.demo.sales.ui.components.ResultScreen
import org.calypsonet.keyple.demo.sales.ui.nav.Home
import org.calypsonet.keyple.demo.sales.ui.nav.ReadCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun SuccessScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    appState: AppState,
) {
  // Back to the card reading, for the loading of another ticket (as the Android client), replacing
  // the screens of the transaction (back arrow or automatic): the back arrow of the card reading
  // then returns to the home screen
  ResultScreen(
      navController = navController,
      appState = appState,
      kind = ResultKind.SUCCESS,
      message = stringResource(Res.string.contract_loaded),
      onReturn = { navController.navigate(ReadCard) { popUpTo(Home) } },
      modifier = modifier,
  )
}

/**
 * Animation of an operation, followed by its message (in the style of the messages of the Android
 * client). [animationModifier] sets the size of the animation.
 */
@Composable
expect fun PlatformAnimation(
    message: String,
    textColor: Color,
    composition: LottieComposition?,
    progress: Float,
    infinite: Boolean = true,
    animationModifier: Modifier = Modifier,
)
