package org.calypsonet.keyple.demo.validation.adapter.primary.ui.base

import android.widget.Toast
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.adapter.secondary.reader.CardSelectionService
import org.calypsonet.keyple.demo.validation.adapter.secondary.repository.LocationAdapter
import org.calypsonet.keyple.demo.validation.domain.port.input.AnalyzeCardSelectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.CleanupReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.InitializeReaderUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.StartCardDetectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.StopCardDetectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.input.ValidateCardUseCase

/**
 * Base activity for the hexagonal architecture.
 * Injects use cases instead of direct services.
 */
abstract class BaseActivity : DaggerAppCompatActivity() {

    @Inject lateinit var initializeReaderUseCase: InitializeReaderUseCase
    @Inject lateinit var startCardDetectionUseCase: StartCardDetectionUseCase
    @Inject lateinit var stopCardDetectionUseCase: StopCardDetectionUseCase
    @Inject lateinit var analyzeCardSelectionUseCase: AnalyzeCardSelectionUseCase
    @Inject lateinit var validateCardUseCase: ValidateCardUseCase
    @Inject lateinit var cleanupReaderUseCase: CleanupReaderUseCase

    @Inject lateinit var locationAdapter: LocationAdapter
    @Inject lateinit var cardSelectionService: CardSelectionService

    fun showToast(message: String) {
        runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }
}
