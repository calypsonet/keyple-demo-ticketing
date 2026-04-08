package org.calypsonet.keyple.demo.reload.remote.domain.spi

import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto
import org.calypsonet.keyple.demo.common.dto.CardIssuanceOutputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto
import org.calypsonet.keyple.demo.common.dto.WriteContractOutputDto
import org.eclipse.keypop.reader.selection.spi.SmartCard

interface RemoteServiceManager {
    fun analyzeContracts(
        localReaderName: String,
        smartCard: SmartCard,
        input: AnalyzeContractsInputDto
    ): AnalyzeContractsOutputDto

    fun personalizeCard(
        localReaderName: String,
        smartCard: SmartCard,
        input: CardIssuanceInputDto
    ): CardIssuanceOutputDto

    fun writeContract(
        localReaderName: String,
        smartCard: SmartCard,
        input: WriteContractInputDto
    ): WriteContractOutputDto
}
