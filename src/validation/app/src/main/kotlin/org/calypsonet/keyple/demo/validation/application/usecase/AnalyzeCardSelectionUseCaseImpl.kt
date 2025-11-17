package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.model.CardSelectionResult
import org.calypsonet.keyple.demo.validation.domain.port.input.AnalyzeCardSelectionUseCase
import org.calypsonet.keyple.demo.validation.domain.port.output.CardRepository
import timber.log.Timber

/**
 * Implementation of the analyze card selection use case.
 * Determines the type of detected card and whether it's valid.
 */
class AnalyzeCardSelectionUseCaseImpl
@Inject
constructor(
    private val cardRepository: CardRepository
) : AnalyzeCardSelectionUseCase {

    override suspend fun analyze(): CardSelectionResult {
        return try {
            val cardType = cardRepository.analyzeCardSelection()
            CardSelectionResult(
                cardType = cardType,
                isValid = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze card selection")
            CardSelectionResult(
                cardType = org.calypsonet.keyple.demo.validation.domain.model.CardType.UNKNOWN,
                isValid = false,
                errorMessage = e.message
            )
        }
    }
}
