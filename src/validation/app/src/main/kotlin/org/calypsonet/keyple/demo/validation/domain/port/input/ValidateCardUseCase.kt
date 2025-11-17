package org.calypsonet.keyple.demo.validation.domain.port.input

import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult

/**
 * Use case for validating a transportation card.
 * This is the core business logic that performs the validation procedure.
 */
interface ValidateCardUseCase {
    /**
     * Execute the validation procedure on the currently selected card.
     * This includes:
     * - Reading card environment and contracts
     * - Checking validity dates
     * - Verifying anti-passback rules
     * - Selecting the best contract
     * - Decrementing counters if needed
     * - Writing validation event to card
     *
     * @param validationAmount The amount to charge (optional, for future use)
     * @return ValidationResult containing validation outcome and updated card state
     */
    suspend fun validate(validationAmount: Int? = null): ValidationResult
}
