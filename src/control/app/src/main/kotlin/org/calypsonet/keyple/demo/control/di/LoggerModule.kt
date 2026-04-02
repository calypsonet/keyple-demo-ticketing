package org.calypsonet.keyple.demo.control.di

import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.control.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.ui.adapters.LoggerImpl

@Suppress("unused")
@Module
class LoggerModule {

    @Provides @AppScoped
    fun provideLogger(): Logger = LoggerImpl()
}