package org.calypsonet.keyple.demo.validation.application.usecase

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.exception.ValidationException
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.port.input.ValidateCardUseCase
import org.calypsonet.keyple.demo.validation.domain.port.output.CardRepository
import org.calypsonet.keyple.demo.validation.domain.port.output.LocationProvider
import org.calypsonet.keyple.demo.validation.domain.port.output.UiFeedbackPort
import timber.log.Timber

/**
 * Implementation of the validate card use case.
 * This orchestrates the complete validation workflow:
 * 1. Get current location
 * 2. Execute validation on the card (delegated to CardRepository)
 * 3. Provide UI feedback
 *
 * The actual business rules (anti-passback, contract selection, etc.)
 * are executed within the CardRepository adapter.
 */
class ValidateCardUseCaseImpl
@Inject
constructor(
    private val cardRepository: CardRepository,
    private val locationProvider: LocationProvider,
    private val uiFeedbackPort: UiFeedbackPort
) : ValidateCardUseCase {

    override suspend fun validate(validationAmount: Int?): ValidationResult {
        return try {
            // Get current location
            val location = locationProvider.getCurrentLocation()

            // Execute validation procedure
            // This delegates to the appropriate adapter (Calypso or Storage Card)
            // which will apply all business rules and perform the validation
            val result = cardRepository.executeValidation(
                locationId = location.id,
                locationName = location.name,
                validationAmount = validationAmount
            )

            // Provide feedback based on result
            when (result.status) {
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.SUCCESS -> {
                    uiFeedbackPort.displaySuccess()
                    Timber.i("Validation successful: ${result.contractName}")
                }
                else -> {
                    uiFeedbackPort.displayFailure()
                    Timber.w("Validation failed: ${result.status} - ${result.errorMessage}")
                }
            }

            result
        } catch (e: ValidationException) {
            Timber.e(e, "Validation exception")
            uiFeedbackPort.displayFailure()
            ValidationResult.error(
                cardType = org.calypsonet.keyple.demo.validation.domain.model.CardType.UNKNOWN,
                errorMessage = e.message ?: "Validation failed"
            )
        } catch (e: Exception) {
            Timber.e(e, "Unexpected validation error")
            uiFeedbackPort.displayFailure()
            ValidationResult.error(
                cardType = org.calypsonet.keyple.demo.validation.domain.model.CardType.UNKNOWN,
                errorMessage = "Unexpected error: ${e.message}"
            )
        }
    }
}
