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
package org.calypsonet.keyple.demo.validation.data

import org.eclipse.keyple.core.util.HexUtil

/**
 * Provides Mifare Classic authentication keys for Android NFC and Bluebird plugins. For demo
 * purposes, returns the factory default key (FF FF FF FF FF FF).
 *
 * In production, this should be replaced with a secure key management system using Android KeyStore
 * or a similar secure storage mechanism.
 */
class MifareClassicKeyProviderImpl :
    org.eclipse.keyple.plugin.android.nfc.spi.KeyProvider,
    org.calypsonet.keyple.plugin.bluebird.spi.KeyProvider {
  override fun getKey(keyNumber: Int): ByteArray? {
    // Returns factory default key for demonstration purposes.
    // TODO: In production, implement secure key storage (Android KeyStore or equivalent).
    return HexUtil.toByteArray("FFFFFFFFFFFF")
  }
}
