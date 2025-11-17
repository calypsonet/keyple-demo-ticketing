package org.calypsonet.keyple.demo.validation.domain.exception

/**
 * Base exception for validation business rule violations.
 */
sealed class ValidationException(message: String) : Exception(message) {
    /**
     * Thrown when card environment is expired or invalid.
     */
    class ExpiredEnvironment(message: String = "Card environment has expired") :
        ValidationException(message)

    /**
     * Thrown when anti-passback rule is violated (validation too soon).
     */
    class AntiPassbackViolation(message: String = "Validation too soon after previous validation") :
        ValidationException(message)

    /**
     * Thrown when no valid contract is found on the card.
     */
    class NoValidContract(message: String = "No valid contract found on card") :
        ValidationException(message)

    /**
     * Thrown when card structure or version is invalid.
     */
    class InvalidCardStructure(message: String) :
        ValidationException(message)

    /**
     * Thrown when card is empty (no contracts).
     */
    class EmptyCard(message: String = "Card has no contracts") :
        ValidationException(message)
}
