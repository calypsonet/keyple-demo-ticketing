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

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.reload.remote.Application
import org.calypsonet.keyple.demo.reload.remote.data.AppSettingsRepositoryImpl
import org.calypsonet.keyple.demo.reload.remote.data.network.ServerStatusProviderImpl
import org.calypsonet.keyple.demo.reload.remote.di.scopes.AppScoped
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider

@Suppress("unused")
@Module
class DataModule {

  @Provides
  @AppScoped
  fun getSharedPreferences(app: Application): SharedPreferences {
    return app.getSharedPreferences("Keyple-prefs", Context.MODE_PRIVATE)
  }

  @Provides
  @AppScoped
  fun provideAppSettingsRepository(prefs: SharedPreferences): AppSettingsRepository =
      AppSettingsRepositoryImpl(prefs)

  @Provides
  @AppScoped
  fun provideServerStatusProvider(): ServerStatusProvider = ServerStatusProviderImpl()
}
