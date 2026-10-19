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
package org.calypsonet.keyple.demo.sales.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.sales.domain.CardReaderSession
import org.calypsonet.keyple.demo.sales.domain.spi.Clock
import org.calypsonet.keyple.demo.sales.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.sales.domain.spi.Logger
import org.calypsonet.keyple.demo.sales.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.sales.domain.spi.TicketingServer
import org.calypsonet.keyple.demo.sales.domain.usecases.LoadContract
import org.calypsonet.keyple.demo.sales.domain.usecases.PersonalizeCard
import org.calypsonet.keyple.demo.sales.domain.usecases.ReadContracts

/** Provides the domain services, which carry no dependency injection annotation. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

  @Provides
  @Singleton
  fun provideCardReaderSession(
      keypopApiProvider: KeypopApiProvider,
      terminalReaders: TerminalReaders,
      logger: Logger
  ): CardReaderSession = CardReaderSession(keypopApiProvider, terminalReaders, logger)

  @Provides
  @Singleton
  fun provideReadContracts(
      cardReaderSession: CardReaderSession,
      ticketingServer: TicketingServer,
      clock: Clock,
      logger: Logger
  ): ReadContracts = ReadContracts(cardReaderSession, ticketingServer, clock, logger)

  @Provides
  @Singleton
  fun provideLoadContract(
      cardReaderSession: CardReaderSession,
      ticketingServer: TicketingServer,
      logger: Logger
  ): LoadContract = LoadContract(cardReaderSession, ticketingServer, logger)

  @Provides
  @Singleton
  fun providePersonalizeCard(
      cardReaderSession: CardReaderSession,
      ticketingServer: TicketingServer,
      logger: Logger
  ): PersonalizeCard = PersonalizeCard(cardReaderSession, ticketingServer, logger)
}
