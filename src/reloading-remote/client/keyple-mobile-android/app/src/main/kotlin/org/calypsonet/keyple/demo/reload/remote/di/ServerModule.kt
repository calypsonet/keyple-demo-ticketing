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
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.calypsonet.keyple.demo.reload.remote.BuildConfig
import org.calypsonet.keyple.demo.reload.remote.data.KeypleTicketingServer
import org.calypsonet.keyple.demo.reload.remote.data.network.KeypleSyncEndPointClient
import org.calypsonet.keyple.demo.reload.remote.data.network.RestServerStatusProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ServerStatusProvider
import org.calypsonet.keyple.demo.reload.remote.domain.spi.TicketingServer
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.distributed.LocalServiceClient
import org.eclipse.keyple.distributed.LocalServiceClientFactoryBuilder

/**
 * Communication with the server: Keyple Distributed local service and ticketing server (remote
 * services), SAM status of the server (REST API).
 */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class ServerModule {

  @Provides
  @Singleton
  fun provideKeypleSyncEndpointClient(
      appSettings: AppSettingsRepository,
      logger: Logger
  ): KeypleSyncEndPointClient {
    // Logs the exchanges with the server (messages of the remote plugin) in the debug builds only
    val loggingInterceptor =
        HttpLoggingInterceptor { message -> logger.d(message) }
            .setLevel(
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE)
    return KeypleSyncEndPointClient(
        appSettings,
        OkHttpClient.Builder().addNetworkInterceptor(loggingInterceptor).build(),
        logger)
  }

  @Provides
  @Singleton
  fun provideLocalServiceClient(
      keypleSyncEndPointClient: KeypleSyncEndPointClient
  ): LocalServiceClient {
    val smartCardService = SmartCardServiceProvider.getService()
    val localService =
        smartCardService.getDistributedLocalService("localService")
            ?: smartCardService.registerDistributedLocalService(
                LocalServiceClientFactoryBuilder.builder("localService")
                    .withSyncNode(keypleSyncEndPointClient)
                    .build())
    return localService.getExtension(LocalServiceClient::class.java)
  }

  @Provides
  @Singleton
  fun provideTicketingServer(localServiceClient: LocalServiceClient): TicketingServer =
      KeypleTicketingServer(localServiceClient)

  @Provides
  @Singleton
  fun provideServerStatusProvider(): ServerStatusProvider = RestServerStatusProvider()
}
