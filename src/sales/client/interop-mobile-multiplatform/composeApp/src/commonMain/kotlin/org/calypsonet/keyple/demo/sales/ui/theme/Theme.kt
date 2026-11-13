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
package org.calypsonet.keyple.demo.sales.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import org.calypsonet.keyple.composeapp.generated.resources.Res
import org.calypsonet.keyple.composeapp.generated.resources.worksans_bold
import org.calypsonet.keyple.composeapp.generated.resources.worksans_medium
import org.calypsonet.keyple.composeapp.generated.resources.worksans_regular
import org.calypsonet.keyple.composeapp.generated.resources.worksans_semibold
import org.jetbrains.compose.resources.Font

// Colors of the Android client (colors.xml)
val blue = Color(0xFF1D87BB)
val red = Color(0xFFB7003A)
val green = Color(0xFF1DAB5D)
val orange = Color(0xFFE47A27)
val darkBlue = Color(0xFF213b6c)
val grey = Color(0xFF808080)
val lightBlue = Color(0xFFe8f3f9)
val lineBlue = Color(0xFF8db9dd)
val white = Color(0xFFFFFFFF)

/** Window background of the Android client (light AppCompat theme). */
val background = Color(0xFFFAFAFA)

/**
 * Dimensions of the Android client (resources `values`, `values-sw400dp` and `values-sw600dp`),
 * selected from the smallest width of the screen as Android does.
 */
data class AppDimens(
    val bigText: TextUnit,
    val mediumText: TextUnit,
    val smallText: TextUnit,
    val smallerText: TextUnit,
    val toolbarMarginTop: Dp,
    val readingAnimationSize: Dp,
    val warningTopMargin: Dp,
    val homeCardHeight: Dp,
    val homeCardGap: Dp,
    val homeIconSize: Dp,
    val homeTextWidth: Dp,
    val calypsoLogoWidth: Dp,
    val contractTextWidth: Dp,
)

private val dimensSmall =
    AppDimens(
        bigText = 20.sp,
        mediumText = 18.sp,
        smallText = 17.3.sp,
        smallerText = 12.sp,
        toolbarMarginTop = 10.dp,
        readingAnimationSize = 150.dp,
        warningTopMargin = 10.dp,
        homeCardHeight = 84.dp,
        homeCardGap = 24.dp,
        homeIconSize = 36.dp,
        homeTextWidth = 200.dp,
        calypsoLogoWidth = 163.dp,
        contractTextWidth = 120.dp,
    )

// values-sw400dp: the dimensions it does not define are those of values
private val dimensMedium =
    dimensSmall.copy(
        bigText = 22.sp,
        mediumText = 20.sp,
        smallerText = 15.sp,
        readingAnimationSize = 180.dp,
        warningTopMargin = 20.dp,
        homeCardHeight = 108.dp,
        homeCardGap = 32.dp,
        homeIconSize = 44.dp,
        contractTextWidth = 150.dp,
    )

// values-sw600dp: the dimensions it does not define are those of values-sw400dp
private val dimensLarge =
    dimensMedium.copy(
        bigText = 27.sp,
        mediumText = 23.sp,
        smallText = 20.sp,
        toolbarMarginTop = 40.dp,
        readingAnimationSize = 250.dp,
        homeCardHeight = 168.dp,
        homeIconSize = 56.dp,
        homeTextWidth = 230.dp,
        calypsoLogoWidth = 183.dp,
    )

internal fun appDimens(smallestWidth: Dp): AppDimens =
    when {
      smallestWidth >= 600.dp -> dimensLarge
      smallestWidth >= 400.dp -> dimensMedium
      else -> dimensSmall
    }

val LocalAppDimens = staticCompositionLocalOf { dimensSmall }

/** Text styles of the Android client (styles.xml), sized for the screen. */
object AppTextStyles {
  private val family: FontFamily?
    @Composable get() = MaterialTheme.typography.bodyLarge.fontFamily

  private val dimens: AppDimens
    @Composable get() = LocalAppDimens.current

  /** Messages and titles (`big...` styles). */
  val bigBold: TextStyle
    @Composable
    get() = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = dimens.bigText)

  /** Secondary texts (`medium...` styles). */
  val mediumRegular: TextStyle
    @Composable
    get() =
        TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = dimens.mediumText)

  val mediumBold: TextStyle
    @Composable
    get() =
        TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = dimens.mediumText)

  /** Labels and values (`small...` styles). */
  val smallRegular: TextStyle
    @Composable
    get() =
        TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = dimens.smallText)

  val smallMedium: TextStyle
    @Composable
    get() =
        TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = dimens.smallText)

  val smallBold: TextStyle
    @Composable
    get() =
        TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = dimens.smallText)

  /** Card type and other minor information (`smaller...` styles). */
  val smallerRegular: TextStyle
    @Composable
    get() =
        TextStyle(
            fontFamily = family, fontWeight = FontWeight.Normal, fontSize = dimens.smallerText)
}

@Composable
private fun workSans(): FontFamily =
    FontFamily(
        Font(Res.font.worksans_regular, FontWeight.Normal),
        Font(Res.font.worksans_medium, FontWeight.Medium),
        Font(Res.font.worksans_semibold, FontWeight.SemiBold),
        Font(Res.font.worksans_bold, FontWeight.Bold),
    )

@Composable
private fun workSansTypography(): Typography {
  val family = workSans()
  return with(Typography()) {
    copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
  }
}

// The Android client has no dark theme: the same colors are used whatever the system theme
private val colors =
    lightColorScheme(
        primary = blue,
        onPrimary = white,
        background = background,
        onBackground = darkBlue,
        surface = background,
        onSurface = darkBlue,
    )

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = colors, typography = workSansTypography()) {
    BoxWithConstraints {
      CompositionLocalProvider(
          LocalAppDimens provides appDimens(min(maxWidth, maxHeight)), content = content)
    }
  }
}
