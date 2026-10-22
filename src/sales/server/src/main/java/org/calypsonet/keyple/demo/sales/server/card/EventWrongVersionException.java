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
package org.calypsonet.keyple.demo.sales.server.card;

/**
 * Thrown when the last event of the card has neither the current version nor the undefined one
 * (clean card).
 */
class EventWrongVersionException extends RuntimeException {
  EventWrongVersionException() {
    super("Event of an unexpected version");
  }
}
