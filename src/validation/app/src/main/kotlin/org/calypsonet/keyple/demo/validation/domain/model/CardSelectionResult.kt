package org.calypsonet.keyple.demo.validation.domain.model

/**
 * Result of card selection analysis.
 */
data class CardSelectionResult(
    val cardType: CardType,
    val isValid: Boolean,
    val errorMessage: String? = null
)
