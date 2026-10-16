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
package org.calypsonet.keyple.demo.common.codecs

import fr.devnied.bitlib.BitUtils
import org.calypsonet.keyple.demo.common.model.ContractStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/**
 * Codec of the contract record of the storage cards (16 bytes), which also contains the counter of
 * the contract. The SAM fields of the contract are not stored.
 */
object StorageCardContractCodec : RecordCodec<ContractStructure> {

  private const val CONTRACT_SIZE_BITS = 128
  private const val CONTRACT_VERSION_NUMBER_SIZE_BITS = 8
  private const val CONTRACT_TARIFF_SIZE_BITS = 8
  private const val CONTRACT_SALE_DATE_SIZE_BITS = 16
  private const val CONTRACT_VALIDITY_END_DATE_SIZE_BITS = 16
  private const val CONTRACT_AUTH_KVC_SIZE_BITS = 8
  private const val CONTRACT_AUTHENTICATOR_SIZE_BITS = 24
  private const val CONTRACT_PADDING_BITS = 16
  private const val CONTRACT_COUNTER_PADDING_BITS = 8
  private const val CONTRACT_COUNTER_SIZE_BITS = 24

  override fun decode(content: ByteArray): ContractStructure {
    val bitUtils = BitUtils(content)
    val contractVersionNumber =
        VersionNumber.fromCode(bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE_BITS))
    val contractTariff = PriorityCode.fromCode(bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE_BITS))
    val contractSaleDate = DateCompact(bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE_BITS))
    val contractValidityEndDate =
        DateCompact(bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE_BITS))
    val contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE_BITS)
    val contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE_BITS)
    bitUtils.addCurrentBitIndex(CONTRACT_PADDING_BITS + CONTRACT_COUNTER_PADDING_BITS)
    return ContractStructure(
        contractVersionNumber = contractVersionNumber,
        contractTariff = contractTariff,
        contractSaleDate = contractSaleDate,
        contractValidityEndDate = contractValidityEndDate,
        contractSaleSam = null,
        contractSaleCounter = null,
        contractAuthKvc = contractAuthKvc,
        contractAuthenticator = contractAuthenticator,
        counterValue = bitUtils.getNextInteger(CONTRACT_COUNTER_SIZE_BITS))
  }

  override fun encode(structure: ContractStructure): ByteArray {
    val bitUtils = BitUtils(CONTRACT_SIZE_BITS)
    bitUtils.setNextValue(structure.contractVersionNumber.code, CONTRACT_VERSION_NUMBER_SIZE_BITS)
    bitUtils.setNextValue(structure.contractTariff.code, CONTRACT_TARIFF_SIZE_BITS)
    bitUtils.setNextValue(structure.contractSaleDate.value, CONTRACT_SALE_DATE_SIZE_BITS)
    bitUtils.setNextValue(
        structure.contractValidityEndDate.value, CONTRACT_VALIDITY_END_DATE_SIZE_BITS)
    bitUtils.setNextValue(structure.contractAuthKvc ?: 0, CONTRACT_AUTH_KVC_SIZE_BITS)
    bitUtils.setNextValue(structure.contractAuthenticator ?: 0, CONTRACT_AUTHENTICATOR_SIZE_BITS)
    bitUtils.setNextValue(0, CONTRACT_PADDING_BITS + CONTRACT_COUNTER_PADDING_BITS)
    bitUtils.setNextValue(structure.counterValue ?: 0, CONTRACT_COUNTER_SIZE_BITS)
    return bitUtils.data
  }
}
