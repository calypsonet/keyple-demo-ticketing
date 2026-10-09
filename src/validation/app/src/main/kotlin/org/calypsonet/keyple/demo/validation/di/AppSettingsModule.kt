/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.validation.data.AppSettingsRepositoryImpl
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class AppSettingsModule {

  @Provides
  @Singleton
  fun provideAppSettingsRepository(): AppSettingsRepository = AppSettingsRepositoryImpl()
}
