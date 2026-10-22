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
package org.calypsonet.keyple.demo.inspection.domain.model

/** Reason why the presented card cannot be selected. */
enum class SelectionError {
  /** No supported application or card type has been found. */
  CARD_NOT_RECOGNIZED,
  /** The DF name of the Calypso application is not the expected one. */
  UNEXPECTED_DF_NAME,
  /** The file structure of the Calypso application is not supported. */
  UNSUPPORTED_FILE_STRUCTURE
}
