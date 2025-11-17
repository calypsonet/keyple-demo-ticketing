package org.calypsonet.keyple.demo.validation.adapter.secondary.repository

import android.content.Context
import java.time.LocalDateTime
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.CalypsoCardRepository
import org.calypsonet.keyple.demo.validation.data.ReaderRepository
import org.calypsonet.keyple.demo.validation.data.StorageCardRepository
import org.calypsonet.keyple.demo.validation.domain.model.CardData
import org.calypsonet.keyple.demo.validation.domain.model.CardType
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.port.output.CardRepository
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.WriteAccessLevel
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam
import org.eclipse.keypop.reader.CardReader
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.selection.spi.SmartCard
import org.eclipse.keypop.storagecard.card.StorageCard
import timber.log.Timber

/**
 * Facade implementing CardRepository port.
 * This delegates to the legacy Calypso/Storage repositories and converts
 * between domain models and data models.
 */
class CardRepositoryFacade
@Inject
constructor(
    private val context: Context,
    private val readerRepository: ReaderRepository,
    private val locationAdapter: LocationAdapter
) : CardRepository {

    private val calypsoExtensionService: CalypsoExtensionService =
        CalypsoExtensionService.getInstance()
    private val calypsoCardApiFactory: CalypsoCardApiFactory =
        calypsoExtensionService.calypsoCardApiFactory

    private var currentSmartCard: SmartCard? = null
    private var currentCardType: CardType = CardType.UNKNOWN

    /**
     * Temporary: This will be replaced by proper card selection in the use case
     */
    fun setCurrentCard(smartCard: SmartCard) {
        this.currentSmartCard = smartCard
        this.currentCardType = when (smartCard) {
            is CalypsoCard -> CardType.CALYPSO
            is StorageCard -> CardType.STORAGE_CARD
            else -> CardType.UNKNOWN
        }
    }

    override suspend fun analyzeCardSelection(): CardType {
        return currentCardType
    }

    override suspend fun readCardData(): CardData {
        // Not implemented for now - would require extracting read logic from repositories
        throw NotImplementedError("readCardData not yet implemented - use executeValidation directly")
    }

    override suspend fun executeValidation(
        locationId: Int,
        locationName: String,
        validationAmount: Int?
    ): ValidationResult {
        val smartCard = currentSmartCard
            ?: throw IllegalStateException("No card selected")

        return when (smartCard) {
            is CalypsoCard -> executeCalypsoValidation(smartCard, validationAmount ?: 1)
            is StorageCard -> executeStorageCardValidation(smartCard, validationAmount ?: 1)
            else -> ValidationResult.error(
                cardType = CardType.UNKNOWN,
                errorMessage = "Unknown card type"
            )
        }
    }

    private suspend fun executeCalypsoValidation(
        calypsoCard: CalypsoCard,
        validationAmount: Int
    ): ValidationResult {
        try {
            val cardReader = readerRepository.getCardReader()
                ?: throw IllegalStateException("Card reader not initialized")

            val samReader = readerRepository.getSamReader()
                ?: throw IllegalStateException("SAM reader not initialized")

            // Select and get SAM
            val sam = selectSam(samReader)
                ?: throw IllegalStateException("Failed to select SAM")

            // Build security settings
            val cardSecuritySettings = buildCardSecuritySettings(sam)

            // Execute legacy validation
            val calypsoRepo = CalypsoCardRepository()
            val locations = locationAdapter.getLocations()
            val response = calypsoRepo.executeValidationProcedure(
                validationDateTime = LocalDateTime.now(),
                context = context,
                validationAmount = validationAmount,
                cardReader = cardReader,
                calypsoCard = calypsoCard,
                cardSecuritySettings = cardSecuritySettings,
                locations = locations
            )

            // Convert to domain model
            return response.toDomainValidationResult()
        } catch (e: Exception) {
            Timber.e(e, "Calypso validation failed")
            return ValidationResult.error(
                cardType = CardType.CALYPSO,
                errorMessage = e.message ?: "Validation failed"
            )
        }
    }

    private suspend fun executeStorageCardValidation(
        storageCard: StorageCard,
        validationAmount: Int
    ): ValidationResult {
        try {
            val cardReader = readerRepository.getCardReader()
                ?: throw IllegalStateException("Card reader not initialized")

            // Execute legacy validation
            val storageRepo = StorageCardRepository()
            val locations = locationAdapter.getLocations()
            val response = storageRepo.executeValidationProcedure(
                validationDateTime = LocalDateTime.now(),
                context = context,
                validationAmount = validationAmount,
                cardReader = cardReader,
                storageCard = storageCard,
                locations = locations
            )

            // Convert to domain model
            return response.toDomainValidationResult()
        } catch (e: Exception) {
            Timber.e(e, "Storage card validation failed")
            return ValidationResult.error(
                cardType = CardType.STORAGE_CARD,
                errorMessage = e.message ?: "Validation failed"
            )
        }
    }

    private fun selectSam(samReader: CardReader): LegacySam? {
        try {
            // Create a SAM selection manager
            val smartCardService = org.eclipse.keyple.core.service.SmartCardServiceProvider.getService()
            val readerApiFactory = smartCardService.readerApiFactory
            val samSelectionManager = readerApiFactory.createCardSelectionManager()

            // Create SAM selection using power-on data filter
            val legacySamExtension = LegacySamExtensionService.getInstance()

            // Create basic card selector with power-on data filter for SAM C1
            val cardSelector = readerApiFactory
                .createBasicCardSelector()
                .filterByPowerOnData(
                    LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null)
                )

            // Create LegacySam selection extension
            val samSelectionExtension = legacySamExtension.legacySamApiFactory
                .createLegacySamSelectionExtension()

            samSelectionManager.prepareSelection(cardSelector, samSelectionExtension)

            // SAM communication: run the selection scenario
            val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

            // Get the LegacySam SmartCard resulting from the selection
            return samSelectionResult.activeSmartCard as? LegacySam
        } catch (e: Exception) {
            Timber.e(e, "Failed to select SAM")
            return null
        }
    }

    private fun buildCardSecuritySettings(sam: LegacySam): SymmetricCryptoSecuritySetting {
        val samReader = readerRepository.getSamReader()
            ?: throw IllegalStateException("SAM reader not initialized")

        val legacySamExtension = LegacySamExtensionService.getInstance()

        // Create symmetric crypto card transaction manager factory
        val transactionManagerFactory = legacySamExtension.legacySamApiFactory
            .createSymmetricCryptoCardTransactionManagerFactory(samReader, sam)

        // Create and configure the security settings
        return calypsoCardApiFactory
            .createSymmetricCryptoSecuritySetting(transactionManagerFactory)
            .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, org.calypsonet.keyple.demo.common.constant.CardConstant.DEFAULT_KIF_PERSONALIZATION)
            .assignDefaultKif(WriteAccessLevel.LOAD, org.calypsonet.keyple.demo.common.constant.CardConstant.DEFAULT_KIF_LOAD)
            .assignDefaultKif(WriteAccessLevel.DEBIT, org.calypsonet.keyple.demo.common.constant.CardConstant.DEFAULT_KIF_DEBIT)
            .enableRatificationMechanism()
            .enableMultipleSession()
    }

    // Extension function to convert CardReaderResponse to ValidationResult
    private fun org.calypsonet.keyple.demo.validation.data.model.CardReaderResponse.toDomainValidationResult(): ValidationResult {
        val domainStatus = when (this.status) {
            org.calypsonet.keyple.demo.validation.data.model.Status.LOADING ->
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.LOADING
            org.calypsonet.keyple.demo.validation.data.model.Status.SUCCESS ->
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.SUCCESS
            org.calypsonet.keyple.demo.validation.data.model.Status.INVALID_CARD ->
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.INVALID_CARD
            org.calypsonet.keyple.demo.validation.data.model.Status.EMPTY_CARD ->
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.EMPTY_CARD
            org.calypsonet.keyple.demo.validation.data.model.Status.ERROR ->
                org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus.ERROR
        }

        val domainValidation = this.validation?.let { v ->
            org.calypsonet.keyple.demo.validation.domain.model.ValidationEvent(
                name = v.name,
                location = org.calypsonet.keyple.demo.validation.domain.model.Location(
                    id = v.location.id,
                    name = v.location.name
                ),
                destination = v.destination,
                dateTime = v.dateTime,
                provider = v.provider
            )
        }

        return ValidationResult(
            status = domainStatus,
            cardType = if (this.cardType.contains("CALYPSO")) CardType.CALYPSO else CardType.STORAGE_CARD,
            nbTicketsLeft = this.nbTicketsLeft,
            contractName = this.contract,
            validation = domainValidation,
            eventDateTime = this.eventDateTime,
            passValidityEndDate = this.passValidityEndDate,
            errorMessage = this.errorMessage
        )
    }
}
