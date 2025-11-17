package org.calypsonet.keyple.demo.validation.domain.model

/**
 * Enum representing the outcome status of a card validation.
 */
enum class ValidationStatus {
    /** Validation in progress */
    LOADING,

    /** Validation successful - valid contract found and updated */
    SUCCESS,

    /** Card structure is invalid or corrupted */
    INVALID_CARD,

    /** No valid contracts found on the card */
    EMPTY_CARD,

    /** Error occurred during validation */
    ERROR
}
