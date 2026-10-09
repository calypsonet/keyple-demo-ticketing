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
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/activity")
public class ActivityController {

  @Inject ActivityService activityService;

  /**
   * Returns all the transactions, in their order of arrival.
   *
   * @return A not null list.
   */
  @GET
  @Path("/events")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Activity> getEvents() {
    return activityService.list();
  }

  /**
   * Server-Sent Events stream of the new transactions, each event being a JSON {@link Activity}.
   * Every subscriber (e.g. each open dashboard) receives all the new transactions.
   *
   * @return The stream of the new transactions.
   */
  @GET
  @Path("/stream")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<Activity> stream() {
    return activityService.stream();
  }
}
