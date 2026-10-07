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
package org.calypsonet.keyple.demo.control.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.control.domain.TicketingService
import org.calypsonet.keyple.demo.control.domain.managers.CalypsoCardControlManager
import org.calypsonet.keyple.demo.control.domain.managers.StorageCardControlManager
import org.calypsonet.keyple.demo.control.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.control.domain.spi.UiManager

/** Provides the domain services, which carry no dependency injection annotation. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

  @Provides
  @Singleton
  fun provideCalypsoCardControlManager(
      keypopApiProvider: KeypopApiProvider,
      logger: Logger
  ): CalypsoCardControlManager = CalypsoCardControlManager(keypopApiProvider, logger)

  @Provides
  @Singleton
  fun provideStorageCardControlManager(
      keypopApiProvider: KeypopApiProvider,
      logger: Logger
  ): StorageCardControlManager = StorageCardControlManager(keypopApiProvider, logger)

  @Provides
  @Singleton
  fun provideTicketingService(
      keypopApiProvider: KeypopApiProvider,
      appSettings: AppSettingsRepository,
      readerManager: ReaderManager,
      uiManager: UiManager,
      logger: Logger,
      calypsoCardControlManager: CalypsoCardControlManager,
      storageCardControlManager: StorageCardControlManager
  ): TicketingService =
      TicketingService(
          keypopApiProvider,
          appSettings,
          readerManager,
          uiManager,
          logger,
          calypsoCardControlManager,
          storageCardControlManager)
}
