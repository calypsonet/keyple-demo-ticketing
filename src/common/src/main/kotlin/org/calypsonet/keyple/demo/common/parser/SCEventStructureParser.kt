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
import org.calypsonet.keyple.demo.common.model.EventStructure
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

class SCEventStructureParser : Parser<EventStructure> {

  override fun parse(content: ByteArray): EventStructure {
    val bitUtils = BitUtils(content)
    val eventVersionNumber =
        VersionNumber.findEnumByKey(bitUtils.getNextInteger(EVENT_VERSION_NUMBER_SIZE))
    val eventDateStamp = DateCompact(bitUtils.getNextInteger(EVENT_DATE_STAMP_SIZE))
    val eventTimeStamp = TimeCompact(bitUtils.getNextInteger(EVENT_TIME_STAMP_SIZE))
    val eventLocation = bitUtils.getNextInteger(EVENT_LOCATION_SIZE)
    val eventContractUsed = bitUtils.getNextInteger(EVENT_CONTRACT_USED_SIZE)
    val contractPriority1 =
        PriorityCode.findEnumByKey(bitUtils.getNextInteger(EVENT_CONTRACT_PRIORITY_SIZE))
    val contractPriority2 =
        PriorityCode.findEnumByKey(bitUtils.getNextInteger(EVENT_CONTRACT_PRIORITY_SIZE))
    val contractPriority3 =
        PriorityCode.findEnumByKey(bitUtils.getNextInteger(EVENT_CONTRACT_PRIORITY_SIZE))
    val contractPriority4 =
        PriorityCode.findEnumByKey(bitUtils.getNextInteger(EVENT_CONTRACT_PRIORITY_SIZE))
    return EventStructure(
        eventVersionNumber = eventVersionNumber,
        eventDateStamp = eventDateStamp,
        eventTimeStamp = eventTimeStamp,
        eventLocation = eventLocation,
        eventContractUsed = eventContractUsed,
        contractPriority1 = contractPriority1,
        contractPriority2 = contractPriority2,
        contractPriority3 = contractPriority3,
        contractPriority4 = contractPriority4)
  }

  override fun generate(content: EventStructure): ByteArray {
    val bitUtils = BitUtils(EVENT_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.eventVersionNumber.key.toLong()).toByteArray(),
        EVENT_VERSION_NUMBER_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.eventDateStamp.value.toLong()).toByteArray(),
        EVENT_DATE_STAMP_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.eventTimeStamp.value.toLong()).toByteArray(),
        EVENT_TIME_STAMP_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.eventLocation.toLong()).toByteArray(), EVENT_LOCATION_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.eventContractUsed.toLong()).toByteArray(),
        EVENT_CONTRACT_USED_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractPriority1.key.toLong()).toByteArray(),
        EVENT_CONTRACT_PRIORITY_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractPriority2.key.toLong()).toByteArray(),
        EVENT_CONTRACT_PRIORITY_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractPriority3.key.toLong()).toByteArray(),
        EVENT_CONTRACT_PRIORITY_SIZE)
    bitUtils.setNextByte(
        BigInteger.valueOf(content.contractPriority4.key.toLong()).toByteArray(),
        EVENT_CONTRACT_PRIORITY_SIZE)
    bitUtils.setNextByte(BigInteger.valueOf(0).toByteArray(), EVENT_PADDING)
    return bitUtils.data
  }

  companion object {
    const val EVENT_SIZE = 128
    const val EVENT_VERSION_NUMBER_SIZE = 8
    const val EVENT_DATE_STAMP_SIZE = 16
    const val EVENT_TIME_STAMP_SIZE = 16
    const val EVENT_LOCATION_SIZE = 32
    const val EVENT_CONTRACT_USED_SIZE = 8
    const val EVENT_CONTRACT_PRIORITY_SIZE = 8
    const val EVENT_PADDING = 16
  }
}
