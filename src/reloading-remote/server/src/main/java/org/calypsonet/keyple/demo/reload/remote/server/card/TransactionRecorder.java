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
package org.calypsonet.keyple.demo.reload.remote.server.card;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.calypsonet.keyple.demo.reload.remote.server.transaction.Transaction;
import org.calypsonet.keyple.demo.reload.remote.server.transaction.TransactionService;

/** Records the card transactions in the transaction log displayed by the dashboard. */
@ApplicationScoped
public class TransactionRecorder {

  // Types of transaction
  static final String READ = "READ";
  static final String SECURED_READ = "SECURED READ";
  static final String RELOAD = "RELOAD";
  static final String PERSONALIZATION = "PERSONALIZATION";

  private static final String SUCCESS = "SUCCESS";
  private static final String FAIL = "FAIL";

  @Inject TransactionService transactionService;

  /**
   * Records a transaction.
   *
   * @param plugin Name of the plugin of the client.
   * @param success True if the transaction succeeded.
   * @param type Type of the transaction.
   * @param cardSerialNumber Serial number of the card.
   */
  void record(String plugin, boolean success, String type, String cardSerialNumber) {
    record(plugin, success, type, cardSerialNumber, null);
  }

  /**
   * Records a transaction loading a contract.
   *
   * @param plugin Name of the plugin of the client.
   * @param success True if the transaction succeeded.
   * @param type Type of the transaction.
   * @param cardSerialNumber Serial number of the card.
   * @param contractLoaded Description of the contract loaded, null if not relevant.
   */
  void record(
      String plugin, boolean success, String type, String cardSerialNumber, String contractLoaded) {
    transactionService.push(
        new Transaction()
            .setPlugin(plugin)
            .setStatus(success ? SUCCESS : FAIL)
            .setType(type)
            .setCardSerialNumber(cardSerialNumber)
            .setContractLoaded(contractLoaded));
  }
}
