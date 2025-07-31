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
package org.calypsonet.keyple.demo.common.model

import java.io.Serializable
import java.time.LocalDateTime
import org.calypsonet.keyple.demo.common.model.type.DateCompact
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.common.model.type.TimeCompact
import org.calypsonet.keyple.demo.common.model.type.VersionNumber

class EventStructure(
    var eventVersionNumber: VersionNumber,
    var eventDateStamp: DateCompact,
    var eventTimeStamp: TimeCompact,
    var eventLocation: Int,
    var eventContractUsed: Int,
    var contractPriority1: PriorityCode,
    var contractPriority2: PriorityCode,
    var contractPriority3: PriorityCode,
    var contractPriority4: PriorityCode
) : Serializable {
  val eventDatetime: LocalDateTime =
      eventDateStamp.getDate().atStartOfDay().plusMinutes(eventTimeStamp.value.toLong())
}
