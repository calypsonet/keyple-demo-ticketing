package org.calypsonet.keyple.demo.validation.domain.model

import org.calypsonet.keyple.demo.common.model.EventStructure

/**
 * Domain model containing all data read from a card.
 */
data class CardData(
    val environment: CardEnvironment,
    val contracts: List<Contract>,
    val lastEvent: EventStructure?
)
