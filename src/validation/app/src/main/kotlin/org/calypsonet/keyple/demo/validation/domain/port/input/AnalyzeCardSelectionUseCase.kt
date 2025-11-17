package org.calypsonet.keyple.demo.validation.domain.port.input

import org.calypsonet.keyple.demo.validation.domain.model.CardSelectionResult

/**
 * Use case for analyzing a detected card selection result.
 */
interface AnalyzeCardSelectionUseCase {
    /**
     * Analyze the card selection result to determine card type and validity.
     *
     * @return CardSelectionResult containing the card type and whether it's valid
     */
    suspend fun analyze(): CardSelectionResult
}
