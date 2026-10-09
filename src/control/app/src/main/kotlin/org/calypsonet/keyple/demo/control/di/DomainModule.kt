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
import org.calypsonet.keyple.demo.control.domain.procedures.CalypsoCardControlProcedure
import org.calypsonet.keyple.demo.control.domain.procedures.StorageCardControlProcedure
import org.calypsonet.keyple.demo.control.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.control.domain.spi.UserFeedback

/** Provides the domain services, which carry no dependency injection annotation. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

  @Provides
  @Singleton
  fun provideTicketingService(
      keypopApiProvider: KeypopApiProvider,
      appSettings: AppSettingsRepository,
      readerManager: ReaderManager,
      userFeedback: UserFeedback,
      logger: Logger
  ): TicketingService =
      TicketingService(
          keypopApiProvider,
          appSettings,
          readerManager,
          userFeedback,
          logger,
          // Control procedures of the supported card technologies
          listOf(
              CalypsoCardControlProcedure(keypopApiProvider, logger),
              StorageCardControlProcedure(keypopApiProvider, logger)))
}
