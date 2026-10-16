/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
 * Codec of the contract record of the Calypso cards (29 bytes). The counter of the contract is
 * stored in a separate file: it is not part of the record.
 */
object CalypsoContractCodec : RecordCodec<ContractStructure> {

  private const val CONTRACT_SIZE_BITS = 232
  private const val CONTRACT_VERSION_NUMBER_SIZE_BITS = 8
  private const val CONTRACT_TARIFF_SIZE_BITS = 8
  private const val CONTRACT_SALE_DATE_SIZE_BITS = 16
  private const val CONTRACT_VALIDITY_END_DATE_SIZE_BITS = 16
  private const val CONTRACT_SALE_SAM_SIZE_BITS = 32
  private const val CONTRACT_SALE_COUNTER_SIZE_BITS = 24
  private const val CONTRACT_AUTH_KVC_SIZE_BITS = 8
  private const val CONTRACT_AUTHENTICATOR_SIZE_BITS = 24
  private const val CONTRACT_PADDING_BITS = 96

  override fun decode(content: ByteArray): ContractStructure {
    val bitUtils = BitUtils(content)
    return ContractStructure(
        contractVersionNumber =
            VersionNumber.fromCode(bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE_BITS)),
        contractTariff = PriorityCode.fromCode(bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE_BITS)),
        contractSaleDate = DateCompact(bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE_BITS)),
        contractValidityEndDate =
            DateCompact(bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE_BITS)),
        contractSaleSam = bitUtils.getNextInteger(CONTRACT_SALE_SAM_SIZE_BITS),
        contractSaleCounter = bitUtils.getNextInteger(CONTRACT_SALE_COUNTER_SIZE_BITS),
        contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE_BITS),
        contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE_BITS))
  }

  override fun encode(structure: ContractStructure): ByteArray {
    val bitUtils = BitUtils(CONTRACT_SIZE_BITS)
    bitUtils.setNextValue(structure.contractVersionNumber.code, CONTRACT_VERSION_NUMBER_SIZE_BITS)
    bitUtils.setNextValue(structure.contractTariff.code, CONTRACT_TARIFF_SIZE_BITS)
    bitUtils.setNextValue(structure.contractSaleDate.value, CONTRACT_SALE_DATE_SIZE_BITS)
    bitUtils.setNextValue(
        structure.contractValidityEndDate.value, CONTRACT_VALIDITY_END_DATE_SIZE_BITS)
    bitUtils.setNextValue(structure.contractSaleSam ?: 0, CONTRACT_SALE_SAM_SIZE_BITS)
    bitUtils.setNextValue(structure.contractSaleCounter ?: 0, CONTRACT_SALE_COUNTER_SIZE_BITS)
    bitUtils.setNextValue(structure.contractAuthKvc ?: 0, CONTRACT_AUTH_KVC_SIZE_BITS)
    bitUtils.setNextValue(structure.contractAuthenticator ?: 0, CONTRACT_AUTHENTICATOR_SIZE_BITS)
    bitUtils.setNextValue(0, CONTRACT_PADDING_BITS)
    return bitUtils.data
  }
}
