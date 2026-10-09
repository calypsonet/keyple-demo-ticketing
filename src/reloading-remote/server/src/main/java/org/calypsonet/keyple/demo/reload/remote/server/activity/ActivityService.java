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
package org.calypsonet.keyple.demo.reload.remote.server.activity;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the transactions and broadcasts each new transaction to all the subscribers (e.g. the
 * dashboards).
 */
@ApplicationScoped
public class ActivityService {

  private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);

  private static final int SUBSCRIBER_BUFFER_SIZE = 100;

  // All the transactions, added by the card processing threads and read by the HTTP threads
  private final List<Activity> activities = new CopyOnWriteArrayList<>();

  // Broadcasts the new transactions, the transactions being pushed from several threads
  private final BroadcastProcessor<Activity> broadcaster = BroadcastProcessor.create();

  /**
   * Returns all the transactions, in their order of arrival.
   *
   * @return A not null list.
   */
  public List<Activity> list() {
    return new ArrayList<>(activities);
  }

  /**
   * Stores a new transaction and broadcasts it to all the subscribers.
   *
   * @param t Transaction object to push.
   */
  public void push(Activity t) {
    activities.add(t);
    synchronized (broadcaster) {
      broadcaster.onNext(t);
    }
    logger.trace("New transaction broadcast: {}", t.getId());
  }

  /**
   * Returns the stream of the new transactions, from the subscription onwards.
   *
   * @return A not null stream.
   */
  public Multi<Activity> stream() {
    return broadcaster.onOverflow().buffer(SUBSCRIBER_BUFFER_SIZE);
  }
}
