package org.calypsonet.keyple.demo.control.data

import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService
import org.calypsonet.keyple.demo.common.constants.CardConstants
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.card.calypso.crypto.pki.CertificateType
import org.eclipse.keyple.card.calypso.crypto.pki.PkiExtensionService
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory
import org.eclipse.keypop.calypso.card.transaction.AsymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.card.transaction.spi.AsymmetricCryptoCardTransactionManagerFactory
import org.eclipse.keypop.calypso.crypto.legacysam.LegacySamApiFactory
import org.eclipse.keypop.reader.ReaderApiFactory
import org.eclipse.keypop.storagecard.StorageCardApiFactory

class KeypopApiProviderImpl : KeypopApiProvider {

    override fun getReaderApiFactory(): ReaderApiFactory {
        return SmartCardServiceProvider.getService().readerApiFactory
    }

    override fun getCalypsoCardApiFactory(): CalypsoCardApiFactory {
        return CalypsoExtensionService.getInstance().calypsoCardApiFactory
    }

    override fun getLegacySamApiFactory(): LegacySamApiFactory {
        return LegacySamExtensionService.getInstance().legacySamApiFactory
    }

    override fun getStorageCardApiFactory(): StorageCardApiFactory {
        return StorageCardExtensionService.getInstance().storageCardApiFactory
    }

    override fun getAsymmetricCryptoSecuritySetting(): AsymmetricCryptoSecuritySetting {
        val pkiExtensionService: PkiExtensionService = PkiExtensionService.getInstance()
        pkiExtensionService.setTestMode()
        val transactionManagerFactory: AsymmetricCryptoCardTransactionManagerFactory? =
            pkiExtensionService.createAsymmetricCryptoCardTransactionManagerFactory()
        val asymmetricCryptoSecuritySetting =
            getCalypsoCardApiFactory().createAsymmetricCryptoSecuritySetting(transactionManagerFactory)
        asymmetricCryptoSecuritySetting
            .addPcaCertificate(
                pkiExtensionService.createPcaCertificate(
                    CardConstants.PCA_PUBLIC_KEY_REFERENCE, CardConstants.PCA_PUBLIC_KEY))
            .addCaCertificate(pkiExtensionService.createCaCertificate(CardConstants.CA_CERTIFICATE))
            .addCaCertificateParser(
                pkiExtensionService.createCaCertificateParser(CertificateType.CALYPSO_LEGACY))
            .addCardCertificateParser(
                pkiExtensionService.createCardCertificateParser(CertificateType.CALYPSO_LEGACY))
        return asymmetricCryptoSecuritySetting
    }
}