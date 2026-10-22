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

/**
 * Encoding and decoding of a record of the card data model (environment and holder, event or
 * contract), for a card technology.
 *
 * @param T The structure of the record.
 */
interface RecordCodec<T> {

  /** Decodes the structure contained in the provided record content. */
  fun decode(content: ByteArray): T

  /** Encodes the provided structure into a record content. */
  fun encode(structure: T): ByteArray
}
