/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.control.domain.procedures

import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.eclipse.keypop.reader.selection.spi.SmartCard

/**
 * Control procedure of a card technology: the procedure applying to the presented card is chosen
 * among the available ones with [supports].
 */
interface ControlProcedure {

  /** Indicates whether the procedure applies to the provided card. */
  fun supports(card: SmartCard): Boolean

  /** Executes the control procedure on the card of the provided context. */
  fun execute(context: ControlContext): ControlResult
}
