package org.calypsonet.keyple.demo.validation.domain.model

import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import java.time.LocalDate

/**
 * Domain model representing a transportation contract with business rules.
 * This is a pure domain entity with no Android dependencies.
 */
data class Contract(
    val index: Int,
    val priorityCode: PriorityCode,
    val validityEndDate: LocalDate,
    val saleDate: LocalDate,
    val counterValue: Int?
) {
    /**
     * Business rule: Check if the contract is valid on the given date.
     */
    fun isValid(currentDate: LocalDate): Boolean {
        return currentDate.isBefore(validityEndDate) ||
               currentDate.isEqual(validityEndDate)
    }

    /**
     * Business rule: Check if the contract has remaining trips/value.
     */
    fun hasTripsRemaining(): Boolean {
        return when (priorityCode) {
            PriorityCode.MULTI_TRIP, PriorityCode.STORED_VALUE ->
                (counterValue ?: 0) > 0
            PriorityCode.SEASON_PASS -> true
            else -> false
        }
    }

    /**
     * Business rule: Check if this contract requires counter decrement.
     */
    fun requiresDecrement(): Boolean {
        return when (priorityCode) {
            PriorityCode.MULTI_TRIP, PriorityCode.STORED_VALUE -> true
            else -> false
        }
    }

    /**
     * Business rule: Decrement the counter.
     * @throws IllegalStateException if counter cannot be decremented
     */
    fun decrementCounter(): Contract {
        require(counterValue != null && counterValue > 0) {
            "Cannot decrement: no trips remaining (counter=$counterValue)"
        }
        return copy(counterValue = counterValue - 1)
    }

    /**
     * Get human-readable contract name.
     */
    fun getContractName(): String {
        return when (priorityCode) {
            PriorityCode.SEASON_PASS -> "Season pass"
            PriorityCode.MULTI_TRIP -> "Multi-trip ticket"
            PriorityCode.STORED_VALUE -> "Stored value"
            else -> "Unknown contract"
        }
    }
}
