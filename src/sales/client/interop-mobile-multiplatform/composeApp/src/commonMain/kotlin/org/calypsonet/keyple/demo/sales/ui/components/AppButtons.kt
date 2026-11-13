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
package org.calypsonet.keyple.demo.sales.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.calypsonet.keyple.demo.sales.ui.theme.AppTextStyles
import org.calypsonet.keyple.demo.sales.ui.theme.blue
import org.calypsonet.keyple.demo.sales.ui.theme.darkBlue
import org.calypsonet.keyple.demo.sales.ui.theme.lightBlue
import org.calypsonet.keyple.demo.sales.ui.theme.white

/**
 * Main action of a screen (buy, pay, save), at the bottom of the screen, as in the Android client
 * (`btn_blue` background, `whiteBtnText` style, `main_btn_*` margins and paddings).
 */
@Composable
fun MainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth().padding(start = 19.dp, end = 19.dp, bottom = 20.dp),
      shape = RoundedCornerShape(5.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = blue,
              contentColor = white,
              disabledContainerColor = blue.copy(alpha = 0.5f),
              disabledContentColor = white),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
      contentPadding = PaddingValues(vertical = 32.dp),
  ) {
    Text(text = text.uppercase(), style = AppTextStyles.mediumBold, textAlign = TextAlign.Center)
  }
}

/**
 * Secondary action of a screen (check of the server state), as in the Android client (light blue
 * button, `smallMediumDarkBlue` style).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier,
      shape = RoundedCornerShape(2.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = lightBlue,
              contentColor = darkBlue,
              disabledContainerColor = lightBlue,
              disabledContentColor = darkBlue.copy(alpha = 0.5f)),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(text = text.uppercase(), style = AppTextStyles.smallMedium)
  }
}
