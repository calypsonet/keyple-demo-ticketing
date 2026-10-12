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
package org.calypsonet.keyple.demo.reload.remote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.reload.remote.domain.CardReaderSession
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Clock
import org.calypsonet.keyple.demo.reload.remote.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager
import org.calypsonet.keyple.demo.reload.remote.domain.usecases.PersonalizeCard
import org.calypsonet.keyple.demo.reload.remote.domain.usecases.ReadContracts
import org.calypsonet.keyple.demo.reload.remote.domain.usecases.ReloadCard

/** Provides the domain services, which carry no dependency injection annotation. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

  @Provides
  @Singleton
  fun provideCardReaderSession(
      keypopApiProvider: KeypopApiProvider,
      readerManager: ReaderManager,
      logger: Logger
  ): CardReaderSession = CardReaderSession(keypopApiProvider, readerManager, logger)

  @Provides
  @Singleton
  fun provideReadContracts(
      cardReaderSession: CardReaderSession,
      remoteServiceManager: RemoteServiceManager,
      clock: Clock
  ): ReadContracts = ReadContracts(cardReaderSession, remoteServiceManager, clock)

  @Provides
  @Singleton
  fun provideReloadCard(
      cardReaderSession: CardReaderSession,
      remoteServiceManager: RemoteServiceManager
  ): ReloadCard = ReloadCard(cardReaderSession, remoteServiceManager)

  @Provides
  @Singleton
  fun providePersonalizeCard(
      cardReaderSession: CardReaderSession,
      remoteServiceManager: RemoteServiceManager
  ): PersonalizeCard = PersonalizeCard(cardReaderSession, remoteServiceManager)
}
