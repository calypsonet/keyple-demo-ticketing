/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.common.parser

import fr.devnied.bitlib.BitUtils
import java.math.BigInteger
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

class SCContractStructureParser : Parser<ContractStructure> {

  override fun parse(content: ByteArray): ContractStructure {
    val bitUtils = BitUtils(content)
    val contractVersionNumber =
        VersionNumber.findEnumByKey(bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE))
    val contractTariff = PriorityCode.findEnumByKey(bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE))
    val contractSaleDate = DateCompact(bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE))
    val contractValidityEndDate =
        DateCompact(bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE))
    val contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE)
    val contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE)
    val contractStructure =
        ContractStructure(
            contractVersionNumber = contractVersionNumber,
            contractTariff = contractTariff,
            contractSaleDate = contractSaleDate,
            contractValidityEndDate = contractValidityEndDate,
            contractSaleSam = null,
            contractSaleCounter = null,
            contractAuthKvc = contractAuthKvc,
            contractAuthenticator = contractAuthenticator)
    bitUtils.addCurrentBitIndex(CONTRACT_PADDING + CONTRACT_COUNTER_PADDING)
    contractStructure.counterValue = bitUtils.getNextInteger(CONTRACT_COUNTER_SIZE)
    return contractStructure
  }

  override fun generate(content: ContractStructure): ByteArray {
    val bitUtils = BitUtils(CONTRACT_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractVersionNumber.key.toLong()).toByteArray(),
        CONTRACT_VERSION_NUMBER_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractTariff.key.toLong()).toByteArray(), CONTRACT_TARIFF_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractSaleDate.value.toLong()).toByteArray(),
        CONTRACT_SALE_DATE_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractValidityEndDate.value.toLong()).toByteArray(),
        CONTRACT_VALIDITY_END_DATE_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractAuthKvc?.toLong() ?: 0).toByteArray(),
        CONTRACT_AUTH_KVC_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractAuthenticator?.toLong() ?: 0).toByteArray(),
        CONTRACT_AUTHENTICATOR_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(0).toByteArray(), CONTRACT_PADDING + CONTRACT_COUNTER_PADDING)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.counterValue?.toLong() ?: 0).toByteArray(),
        CONTRACT_COUNTER_SIZE)
    return bitUtils.data
  }

  companion object {
    const val CONTRACT_SIZE = 128
    const val CONTRACT_VERSION_NUMBER_SIZE = 8
    const val CONTRACT_TARIFF_SIZE = 8
    const val CONTRACT_SALE_DATE_SIZE = 16
    const val CONTRACT_VALIDITY_END_DATE_SIZE = 16
    const val CONTRACT_AUTH_KVC_SIZE = 8
    const val CONTRACT_AUTHENTICATOR_SIZE = 24
    const val CONTRACT_PADDING = 16
    const val CONTRACT_COUNTER_PADDING = 8
    const val CONTRACT_COUNTER_SIZE = 24
  }
}
