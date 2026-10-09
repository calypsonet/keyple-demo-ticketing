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

  private const val CONTRACT_SIZE = 128
  private const val CONTRACT_VERSION_NUMBER_SIZE = 8
  private const val CONTRACT_TARIFF_SIZE = 8
  private const val CONTRACT_SALE_DATE_SIZE = 16
  private const val CONTRACT_VALIDITY_END_DATE_SIZE = 16
  private const val CONTRACT_AUTH_KVC_SIZE = 8
  private const val CONTRACT_AUTHENTICATOR_SIZE = 24
  private const val CONTRACT_PADDING = 16
  private const val CONTRACT_COUNTER_PADDING = 8
  private const val CONTRACT_COUNTER_SIZE = 24

  override fun decode(content: ByteArray): ContractStructure {
    val bitUtils = BitUtils(content)
    val contractVersionNumber =
        VersionNumber.fromCode(bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE))
    val contractTariff = PriorityCode.fromCode(bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE))
    val contractSaleDate = DateCompact(bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE))
    val contractValidityEndDate =
        DateCompact(bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE))
    val contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE)
    val contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE)
    bitUtils.addCurrentBitIndex(CONTRACT_PADDING + CONTRACT_COUNTER_PADDING)
    return ContractStructure(
        contractVersionNumber = contractVersionNumber,
        contractTariff = contractTariff,
        contractSaleDate = contractSaleDate,
        contractValidityEndDate = contractValidityEndDate,
        contractSaleSam = null,
        contractSaleCounter = null,
        contractAuthKvc = contractAuthKvc,
        contractAuthenticator = contractAuthenticator,
        counterValue = bitUtils.getNextInteger(CONTRACT_COUNTER_SIZE))
  }

  override fun encode(structure: ContractStructure): ByteArray {
    val bitUtils = BitUtils(CONTRACT_SIZE)
    bitUtils.setNextValue(structure.contractVersionNumber.code, CONTRACT_VERSION_NUMBER_SIZE)
    bitUtils.setNextValue(structure.contractTariff.code, CONTRACT_TARIFF_SIZE)
    bitUtils.setNextValue(structure.contractSaleDate.value, CONTRACT_SALE_DATE_SIZE)
    bitUtils.setNextValue(structure.contractValidityEndDate.value, CONTRACT_VALIDITY_END_DATE_SIZE)
    bitUtils.setNextValue(structure.contractAuthKvc ?: 0, CONTRACT_AUTH_KVC_SIZE)
    bitUtils.setNextValue(structure.contractAuthenticator ?: 0, CONTRACT_AUTHENTICATOR_SIZE)
    bitUtils.setNextValue(0, CONTRACT_PADDING + CONTRACT_COUNTER_PADDING)
    bitUtils.setNextValue(structure.counterValue ?: 0, CONTRACT_COUNTER_SIZE)
    return bitUtils.data
  }
}
