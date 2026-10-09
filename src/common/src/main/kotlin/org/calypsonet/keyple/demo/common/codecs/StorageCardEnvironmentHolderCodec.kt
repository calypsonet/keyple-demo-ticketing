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
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/** Codec of the environment and holder record of the storage cards (16 bytes). */
object StorageCardEnvironmentHolderCodec : RecordCodec<EnvironmentHolderStructure> {

  private const val ENV_SIZE = 128
  private const val ENV_EVN_SIZE = 8
  private const val ENV_AVN_SIZE = 32
  private const val ENV_ISSUING_DATE_SIZE = 16
  private const val ENV_END_DATE_SIZE = 16
  private const val ENV_HOLDER_COMPANY_SIZE = 8
  private const val ENV_HOLDER_ID_NUMBER_SIZE = 32
  private const val ENV_PADDING = 16

  override fun decode(content: ByteArray): EnvironmentHolderStructure {
    val bitUtils = BitUtils(content)
    return EnvironmentHolderStructure(
        envVersionNumber = VersionNumber.fromCode(bitUtils.getNextInteger(ENV_EVN_SIZE)),
        envApplicationNumber = bitUtils.getNextInteger(ENV_AVN_SIZE),
        envIssuingDate = DateCompact(bitUtils.getNextInteger(ENV_ISSUING_DATE_SIZE)),
        envEndDate = DateCompact(bitUtils.getNextInteger(ENV_END_DATE_SIZE)),
        holderCompany = bitUtils.getNextInteger(ENV_HOLDER_COMPANY_SIZE),
        holderIdNumber = bitUtils.getNextInteger(ENV_HOLDER_ID_NUMBER_SIZE))
  }

  override fun encode(structure: EnvironmentHolderStructure): ByteArray {
    val bitUtils = BitUtils(ENV_SIZE)
    bitUtils.setNextValue(structure.envVersionNumber.code, ENV_EVN_SIZE)
    bitUtils.setNextValue(structure.envApplicationNumber, ENV_AVN_SIZE)
    bitUtils.setNextValue(structure.envIssuingDate.value, ENV_ISSUING_DATE_SIZE)
    bitUtils.setNextValue(structure.envEndDate.value, ENV_END_DATE_SIZE)
    bitUtils.setNextValue(structure.holderCompany ?: 0, ENV_HOLDER_COMPANY_SIZE)
    bitUtils.setNextValue(structure.holderIdNumber ?: 0, ENV_HOLDER_ID_NUMBER_SIZE)
    bitUtils.setNextValue(0, ENV_PADDING)
    return bitUtils.data
  }
}
