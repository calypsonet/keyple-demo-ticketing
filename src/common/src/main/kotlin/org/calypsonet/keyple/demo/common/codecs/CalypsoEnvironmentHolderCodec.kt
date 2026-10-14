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
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/** Codec of the environment and holder record of the Calypso cards (29 bytes). */
object CalypsoEnvironmentHolderCodec : RecordCodec<EnvironmentHolderStructure> {

  private const val ENV_SIZE_BITS = 232
  private const val ENV_VERSION_NUMBER_SIZE_BITS = 8
  private const val ENV_APPLICATION_NUMBER_SIZE_BITS = 32
  private const val ENV_ISSUING_DATE_SIZE_BITS = 16
  private const val ENV_END_DATE_SIZE_BITS = 16
  private const val ENV_HOLDER_COMPANY_SIZE_BITS = 8
  private const val ENV_HOLDER_ID_NUMBER_SIZE_BITS = 32
  private const val ENV_PADDING_BITS = 120

  override fun decode(content: ByteArray): EnvironmentHolderStructure {
    val bitUtils = BitUtils(content)
    return EnvironmentHolderStructure(
        envVersionNumber =
            VersionNumber.fromCode(bitUtils.getNextInteger(ENV_VERSION_NUMBER_SIZE_BITS)),
        envApplicationNumber = bitUtils.getNextInteger(ENV_APPLICATION_NUMBER_SIZE_BITS),
        envIssuingDate = DateCompact(bitUtils.getNextInteger(ENV_ISSUING_DATE_SIZE_BITS)),
        envEndDate = DateCompact(bitUtils.getNextInteger(ENV_END_DATE_SIZE_BITS)),
        holderCompany = bitUtils.getNextInteger(ENV_HOLDER_COMPANY_SIZE_BITS),
        holderIdNumber = bitUtils.getNextInteger(ENV_HOLDER_ID_NUMBER_SIZE_BITS))
  }

  override fun encode(structure: EnvironmentHolderStructure): ByteArray {
    val bitUtils = BitUtils(ENV_SIZE_BITS)
    bitUtils.setNextValue(structure.envVersionNumber.code, ENV_VERSION_NUMBER_SIZE_BITS)
    bitUtils.setNextValue(structure.envApplicationNumber, ENV_APPLICATION_NUMBER_SIZE_BITS)
    bitUtils.setNextValue(structure.envIssuingDate.value, ENV_ISSUING_DATE_SIZE_BITS)
    bitUtils.setNextValue(structure.envEndDate.value, ENV_END_DATE_SIZE_BITS)
    bitUtils.setNextValue(structure.holderCompany ?: 0, ENV_HOLDER_COMPANY_SIZE_BITS)
    bitUtils.setNextValue(structure.holderIdNumber ?: 0, ENV_HOLDER_ID_NUMBER_SIZE_BITS)
    bitUtils.setNextValue(0, ENV_PADDING_BITS)
    return bitUtils.data
  }
}
