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
import timber.log.Timber

/**
 * Provides Mifare Classic authentication keys for both Bluebird and Android NFC plugins.
 *
 * This class implements both KeyProvider interfaces since the authentication keys are
 * card-specific, not plugin-specific.
 *
 * For demo purposes, returns the factory default key (FF FF FF FF FF FF).
 *
 * WARNING: In production, implement secure key storage using Android KeyStore or a secure key
 * management system.
 */
class MifareClassicKeyProvider :
    org.calypsonet.keyple.plugin.bluebird.spi.KeyProvider,
    org.eclipse.keyple.plugin.android.nfc.spi.KeyProvider {

  companion object {
    private const val DEFAULT_KEY = "FFFFFFFFFFFF" // Factory default key
    private const val MAX_KEY_NUMBER = 255 // Mifare Classic key number range: 0-255
  }

  override fun getKey(keyNumber: Int): ByteArray? {
    // Validate key number
    if (keyNumber < 0 || keyNumber > MAX_KEY_NUMBER) {
      Timber.w("Invalid key number requested: $keyNumber (valid range: 0-$MAX_KEY_NUMBER)")
      return null
    }

    // Log key request for debugging (production: remove or use debug level)
    Timber.d("Mifare Classic key requested for keyNumber: $keyNumber")

    // TODO: In production, implement secure key storage based on keyNumber
    // For demo, return factory default key for all keyNumbers
    return HexUtil.toByteArray(DEFAULT_KEY)
  }
}
