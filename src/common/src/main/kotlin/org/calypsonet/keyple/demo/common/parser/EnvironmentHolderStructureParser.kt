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
package org.calypsonet.keyple.demo.common.parser

import fr.devnied.bitlib.BitUtils
import java.math.BigInteger
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

class EnvironmentHolderStructureParser : Parser<EnvironmentHolderStructure> {

  override fun parse(content: ByteArray): EnvironmentHolderStructure {
    val bitUtils = BitUtils(content)
    val envVersionNumber = VersionNumber.findEnumByKey(bitUtils.getNextInteger(ENV_EVN_SIZE))
    val envApplicationNumber = bitUtils.getNextInteger(ENV_AVN_SIZE)
    val envIssuingDate = DateCompact(bitUtils.getNextInteger(ENV_ISSUING_DATE_SIZE))
    val envEndDate = DateCompact(bitUtils.getNextInteger(ENV_END_DATE_SIZE))
    val holderCompany = bitUtils.getNextInteger(ENV_HOLDER_COMPANY_SIZE)
    val holderIdNumber = bitUtils.getNextInteger(ENV_HOLDER_ID_NUMBER_SIZE)
    return EnvironmentHolderStructure(
        envVersionNumber = envVersionNumber,
        envApplicationNumber = envApplicationNumber,
        envIssuingDate = envIssuingDate,
        envEndDate = envEndDate,
        holderCompany = holderCompany,
        holderIdNumber = holderIdNumber)
  }

  override fun generate(content: EnvironmentHolderStructure): ByteArray {
    val bitUtils = BitUtils(ENV_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.envVersionNumber.key.toLong()).toByteArray(), ENV_EVN_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.envApplicationNumber.toLong()).toByteArray(), ENV_AVN_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.envIssuingDate.value.toLong()).toByteArray(),
        ENV_ISSUING_DATE_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.envEndDate.value.toLong()).toByteArray(), ENV_END_DATE_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf((content.holderCompany ?: 0).toLong()).toByteArray(),
        ENV_HOLDER_COMPANY_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf((content.holderIdNumber ?: 0).toLong()).toByteArray(),
        ENV_HOLDER_ID_NUMBER_SIZE)
    bitUtils.setNextByte(BigInteger.valueOf(0).toByteArray(), ENV_PADDING)
    return bitUtils.data
  }

  companion object {
    const val ENV_SIZE = 232
    const val ENV_EVN_SIZE = 8
    const val ENV_AVN_SIZE = 32
    const val ENV_ISSUING_DATE_SIZE = 16
    const val ENV_END_DATE_SIZE = 16
    const val ENV_HOLDER_COMPANY_SIZE = 8
    const val ENV_HOLDER_ID_NUMBER_SIZE = 32
    const val ENV_PADDING = 120
  }
}
