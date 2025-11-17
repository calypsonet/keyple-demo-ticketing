package org.calypsonet.keyple.demo.validation.infrastructure.di

import dagger.Binds
import dagger.Module
import org.calypsonet.keyple.demo.validation.application.usecase.AnalyzeCardSelectionUseCaseImpl
import org.calypsonet.keyple.demo.validation.application.usecase.CleanupReaderUseCaseImpl
import org.calypsonet.keyple.demo.validation.application.usecase.InitializeReaderUseCaseImpl
import org.calypsonet.keyple.demo.validation.application.usecase.StartCardDetectionUseCaseImpl
import org.calypsonet.keyple.demo.validation.application.usecase.StopCardDetectionUseCaseImpl
import org.calypsonet.keyple.demo.validation.application.usecase.ValidateCardUseCaseImpl
import org.calypsonet.keyple.demo.validation.domain.port.input.AnalyzeCardSelectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.CleanupReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.InitializeReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.StartCardDetectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.StopCardDetectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.ValidateCardUseCase
import org.calypsonet.keyple.demo.validation.infrastructure.di.scope.AppScoped

/**
 * Dagger module for binding use case interfaces to their implementations.
 */
@Module
abstract class DomainModule {

    @Binds
    @AppScoped
    abstract fun bindInitializeReaderUseCase(
        impl: InitializeReaderUseCaseImpl
    ): InitializeReaderUseCase

    @Binds
    @AppScoped
    abstract fun bindStartCardDetectionUseCase(
        impl: StartCardDetectionUseCaseImpl
    ): StartCardDetectionUseCase

    @Binds
    @AppScoped
    abstract fun bindStopCardDetectionUseCase(
        impl: StopCardDetectionUseCaseImpl
    ): StopCardDetectionUseCase

    @Binds
    @AppScoped
    abstract fun bindAnalyzeCardSelectionUseCase(
        impl: AnalyzeCardSelectionUseCaseImpl
    ): AnalyzeCardSelectionUseCase

    @Binds
    @AppScoped
    abstract fun bindValidateCardUseCase(
        impl: ValidateCardUseCaseImpl
    ): ValidateCardUseCase

    @Binds
    @AppScoped
    abstract fun bindCleanupReaderUseCase(
        impl: CleanupReaderUseCaseImpl
    ): CleanupReaderUseCase
}
