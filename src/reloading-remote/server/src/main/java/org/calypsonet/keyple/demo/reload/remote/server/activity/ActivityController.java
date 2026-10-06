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

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/activity")
public class ActivityController {

  @Inject ActivityService activityService;

  /**
   * List all events
   *
   * @return not nullable set of events
   */
  @GET
  @Path("/events")
  @Produces(MediaType.APPLICATION_JSON)
  public List<Activity> getEvents() {
    return activityService.list();
  }

  /**
   * Long Polling API to get a new event. HTTP code: 200: a new event is available. HTTP code 204:
   * timeout, please renew request
   *
   * @return a {@link Activity} when a new log is push
   */
  @GET
  @Path("/events/wait")
  @Produces(MediaType.APPLICATION_JSON)
  public Response waitForEvent() {
    Activity t = activityService.waitForNew();
    if (t == null) {
      return Response.noContent().build();
    } else {
      return Response.ok(t).build();
    }
  }
}
