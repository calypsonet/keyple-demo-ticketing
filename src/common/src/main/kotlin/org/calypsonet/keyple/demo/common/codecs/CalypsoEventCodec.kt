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
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

/** Codec of the event record of the Calypso cards (29 bytes). */
object CalypsoEventCodec : RecordCodec<EventStructure> {

  private const val EVENT_SIZE = 232
  private const val EVENT_VERSION_NUMBER_SIZE = 8
  private const val EVENT_DATE_STAMP_SIZE = 16
  private const val EVENT_TIME_STAMP_SIZE = 16
  private const val EVENT_LOCATION_SIZE = 32
  private const val EVENT_CONTRACT_USED_SIZE = 8
  private const val EVENT_CONTRACT_PRIORITY_SIZE = 8
  private const val EVENT_PADDING = 120

  override fun decode(content: ByteArray): EventStructure {
    val bitUtils = BitUtils(content)
    return EventStructure(
        eventVersionNumber =
            VersionNumber.fromCode(bitUtils.getNextInteger(EVENT_VERSION_NUMBER_SIZE)),
        eventDateStamp = DateCompact(bitUtils.getNextInteger(EVENT_DATE_STAMP_SIZE)),
        eventTimeStamp = TimeCompact(bitUtils.getNextInteger(EVENT_TIME_STAMP_SIZE)),
        eventLocation = bitUtils.getNextInteger(EVENT_LOCATION_SIZE),
        eventContractUsed = bitUtils.getNextInteger(EVENT_CONTRACT_USED_SIZE),
        contractPriorities =
            List(EventStructure.CONTRACT_COUNT) {
              PriorityCode.fromCode(bitUtils.getNextInteger(EVENT_CONTRACT_PRIORITY_SIZE))
            })
  }

  override fun encode(structure: EventStructure): ByteArray {
    val bitUtils = BitUtils(EVENT_SIZE)
    bitUtils.setNextValue(structure.eventVersionNumber.code, EVENT_VERSION_NUMBER_SIZE)
    bitUtils.setNextValue(structure.eventDateStamp.value, EVENT_DATE_STAMP_SIZE)
    bitUtils.setNextValue(structure.eventTimeStamp.value, EVENT_TIME_STAMP_SIZE)
    bitUtils.setNextValue(structure.eventLocation, EVENT_LOCATION_SIZE)
    bitUtils.setNextValue(structure.eventContractUsed, EVENT_CONTRACT_USED_SIZE)
    structure.contractPriorities.forEach {
      bitUtils.setNextValue(it.code, EVENT_CONTRACT_PRIORITY_SIZE)
    }
    bitUtils.setNextValue(0, EVENT_PADDING)
    return bitUtils.data
  }
}
