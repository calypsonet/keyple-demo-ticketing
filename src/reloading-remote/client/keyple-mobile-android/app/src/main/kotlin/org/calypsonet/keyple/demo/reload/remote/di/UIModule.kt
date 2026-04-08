/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.calypsonet.keyple.demo.reload.remote.di.scopes.ActivityScoped
import org.calypsonet.keyple.demo.reload.remote.ui.activities.CardReaderActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.CheckoutActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.ConfigurationSettingsActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.HomeActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.MainActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.PaymentValidatedActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.PersonalizationActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.ReloadActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.ReloadResultActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.SelectTicketsActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.ServerSettingsActivity
import org.calypsonet.keyple.demo.reload.remote.ui.activities.SettingsMenuActivity
import org.calypsonet.keyple.demo.reload.remote.ui.cardsummary.CardSummaryActivity

@Suppress("unused")
@Module
abstract class UIModule {

  @ActivityScoped @ContributesAndroidInjector abstract fun mainActivity(): MainActivity?

  @ActivityScoped @ContributesAndroidInjector abstract fun homeActivity(): HomeActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun configurationSettingsActivity(): ConfigurationSettingsActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun serverSettingsActivity(): ServerSettingsActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun settingsMenuActivity(): SettingsMenuActivity?

  @ActivityScoped @ContributesAndroidInjector abstract fun cardReaderActivity(): CardReaderActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun cardSummaryActivity(): CardSummaryActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun selectTicketsActivity(): SelectTicketsActivity?

  @ActivityScoped @ContributesAndroidInjector abstract fun checkoutActivity(): CheckoutActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun paymentValidatedActivity(): PaymentValidatedActivity?

  @ActivityScoped @ContributesAndroidInjector abstract fun chargeCardActivity(): ReloadActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun chargeResultActivity(): ReloadResultActivity?

  @ActivityScoped
  @ContributesAndroidInjector
  abstract fun personalizationActivity(): PersonalizationActivity?
}
