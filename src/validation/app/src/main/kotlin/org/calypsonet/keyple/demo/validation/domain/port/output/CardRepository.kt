package org.calypsonet.keyple.demo.validation.domain.port.output

import org.calypsonet.keyple.demo.validation.domain.model.CardData
import org.calypsonet.keyple.demo.validation.domain.model.CardType
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult

/**
 * Port for card data operations.
 * This abstraction allows the domain to read and write card data without
 * knowing the specific card technology (Calypso, Storage Card, etc.).
 */
interface CardRepository {
    /**
     * Analyze the currently selected card to determine its type.
     *
     * @return CardType indicating the card technology
     */
    suspend fun analyzeCardSelection(): CardType

    /**
     * Read all relevant data from the card (environment, contracts, events).
     *
     * @return CardData containing the parsed card information
     */
    suspend fun readCardData(): CardData

    /**
     * Execute the complete validation procedure on the card.
     * This includes reading, validating business rules, and writing back results.
     *
     * @param locationId The current terminal location ID
     * @param locationName The current terminal location name
     * @param validationAmount Optional validation amount
     * @return ValidationResult with the outcome and updated card state
     */
    suspend fun executeValidation(
        locationId: Int,
        locationName: String,
        validationAmount: Int?
    ): ValidationResult
}
