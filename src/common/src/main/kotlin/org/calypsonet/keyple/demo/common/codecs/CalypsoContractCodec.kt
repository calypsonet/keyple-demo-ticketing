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

  private const val CONTRACT_SIZE = 232
  private const val CONTRACT_VERSION_NUMBER_SIZE = 8
  private const val CONTRACT_TARIFF_SIZE = 8
  private const val CONTRACT_SALE_DATE_SIZE = 16
  private const val CONTRACT_VALIDITY_END_DATE_SIZE = 16
  private const val CONTRACT_SALE_SAM_SIZE = 32
  private const val CONTRACT_SALE_COUNTER_SIZE = 24
  private const val CONTRACT_AUTH_KVC_SIZE = 8
  private const val CONTRACT_AUTHENTICATOR_SIZE = 24
  private const val CONTRACT_PADDING = 96

  override fun decode(content: ByteArray): ContractStructure {
    val bitUtils = BitUtils(content)
    return ContractStructure(
        contractVersionNumber =
            VersionNumber.fromCode(bitUtils.getNextInteger(CONTRACT_VERSION_NUMBER_SIZE)),
        contractTariff = PriorityCode.fromCode(bitUtils.getNextInteger(CONTRACT_TARIFF_SIZE)),
        contractSaleDate = DateCompact(bitUtils.getNextInteger(CONTRACT_SALE_DATE_SIZE)),
        contractValidityEndDate =
            DateCompact(bitUtils.getNextInteger(CONTRACT_VALIDITY_END_DATE_SIZE)),
        contractSaleSam = bitUtils.getNextInteger(CONTRACT_SALE_SAM_SIZE),
        contractSaleCounter = bitUtils.getNextInteger(CONTRACT_SALE_COUNTER_SIZE),
        contractAuthKvc = bitUtils.getNextInteger(CONTRACT_AUTH_KVC_SIZE),
        contractAuthenticator = bitUtils.getNextInteger(CONTRACT_AUTHENTICATOR_SIZE))
  }

  override fun encode(structure: ContractStructure): ByteArray {
    val bitUtils = BitUtils(CONTRACT_SIZE)
    bitUtils.setNextValue(structure.contractVersionNumber.code, CONTRACT_VERSION_NUMBER_SIZE)
    bitUtils.setNextValue(structure.contractTariff.code, CONTRACT_TARIFF_SIZE)
    bitUtils.setNextValue(structure.contractSaleDate.value, CONTRACT_SALE_DATE_SIZE)
    bitUtils.setNextValue(structure.contractValidityEndDate.value, CONTRACT_VALIDITY_END_DATE_SIZE)
    bitUtils.setNextValue(structure.contractSaleSam ?: 0, CONTRACT_SALE_SAM_SIZE)
    bitUtils.setNextValue(structure.contractSaleCounter ?: 0, CONTRACT_SALE_COUNTER_SIZE)
    bitUtils.setNextValue(structure.contractAuthKvc ?: 0, CONTRACT_AUTH_KVC_SIZE)
    bitUtils.setNextValue(structure.contractAuthenticator ?: 0, CONTRACT_AUTHENTICATOR_SIZE)
    bitUtils.setNextValue(0, CONTRACT_PADDING)
    return bitUtils.data
  }
}
