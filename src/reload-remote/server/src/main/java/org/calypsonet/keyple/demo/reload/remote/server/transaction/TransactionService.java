/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.reload.remote.server.transaction;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the transactions and broadcasts each new transaction to all the subscribers (for example,
 * the dashboards).
 */
@ApplicationScoped
public class TransactionService {

  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

  private static final int SUBSCRIBER_BUFFER_SIZE = 100;

  // All the transactions, added by the card processing threads and read by the HTTP threads
  private final List<Transaction> transactions = new CopyOnWriteArrayList<>();

  // Broadcasts the new transactions, the transactions being pushed from several threads
  private final BroadcastProcessor<Transaction> broadcaster = BroadcastProcessor.create();

  /**
   * Returns all the transactions, in their order of arrival.
   *
   * @return A not null list.
   */
  public List<Transaction> list() {
    return new ArrayList<>(transactions);
  }

  /**
   * Stores a new transaction and broadcasts it to all the subscribers.
   *
   * @param transaction Transaction object to push.
   */
  public void push(Transaction transaction) {
    transactions.add(transaction);
    synchronized (broadcaster) {
      broadcaster.onNext(transaction);
    }
    logger.debug("Transaction broadcast [transactionId={}]", transaction.getId());
  }

  /**
   * Returns the stream of the new transactions, from the subscription onwards.
   *
   * @return A not null stream.
   */
  public Multi<Transaction> stream() {
    return broadcaster.onOverflow().buffer(SUBSCRIBER_BUFFER_SIZE);
  }
}
