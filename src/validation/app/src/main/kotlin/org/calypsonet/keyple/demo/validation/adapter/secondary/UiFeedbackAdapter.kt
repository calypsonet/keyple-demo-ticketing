package org.calypsonet.keyple.demo.validation.adapter.secondary

import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.ReaderRepository
import org.calypsonet.keyple.demo.validation.domain.port.output.UiFeedbackPort

/**
 * Adapter implementing UiFeedbackPort using the existing ReaderRepository.
 * Delegates to the reader repository for playing success/error sounds and displaying feedback.
 */
class UiFeedbackAdapter
@Inject
constructor(
    private val readerRepository: ReaderRepository
) : UiFeedbackPort {

    override suspend fun displaySuccess() {
        readerRepository.displayResultSuccess()
    }

    override suspend fun displayFailure() {
        readerRepository.displayResultFailed()
    }
}
