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
 * Thrown when the card has no record available for a new contract (no empty record, nor expired or
 * exhausted contract to replace).
 */
class CardFullException extends RuntimeException {
  CardFullException() {
    super("Card full: no empty, expired, or exhausted contract record");
  }
}
