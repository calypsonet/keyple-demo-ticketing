package org.calypsonet.keyple.demo.validation.domain.model

/**
 * Enum representing the type of card technology.
 */
enum class CardType(val displayName: String) {
    CALYPSO("Calypso Card"),
    STORAGE_CARD("Storage Card"),
    UNKNOWN("Unknown Card")
}
