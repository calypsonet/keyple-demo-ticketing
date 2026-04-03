package org.calypsonet.keyple.demo.reload.remote.di

import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.reload.remote.di.scopes.AppScoped
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.ui.adapters.LoggerImpl

@Suppress("unused")
@Module
class LoggerModule {

    @Provides
    @AppScoped
    fun provideLogger(): Logger = LoggerImpl()
}