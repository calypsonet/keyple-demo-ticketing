/* ******************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
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

import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.RemotePluginServer;
import org.eclipse.keyple.distributed.SyncNodeServer;

@Path("/card")
public class CardController {

  @Inject CardRepository cardRepository;
  @Inject CardSamObserver cardSamObserver;
  @Inject Jsonb jsonb;

  /**
   * Returns the exported card selection scenario, as a JSON string.
   *
   * @return A JSON string containing the exported card selection scenario.
   */
  @GET
  @Path("/export-card-selection-scenario")
  @Produces(MediaType.APPLICATION_JSON)
  public Response exportCardSelectionScenario() {
    String cardSelectionScenarioJsonString = cardRepository.exportCardSelectionScenario();
    return Response.ok(cardSelectionScenarioJsonString).build();
  }

  /**
   * The endpoint access associated with the remote plugin server.
   *
   * <p>The request is read explicitly so that only an unreadable request is answered with "400 Bad
   * Request": an error occurring while processing the message or writing the response remains a
   * server error.
   *
   * @param body The request, as a JSON {@link MessageDto}.
   * @return A list of response messages.
   * @throws BadRequestException If the request is not a valid JSON {@link MessageDto}.
   */
  @POST
  @Path("/remote-plugin")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<MessageDto> processMessage(String body) {

    MessageDto message = readMessage(body);

    // Retrieves the node associated with the remote plugin.
    SyncNodeServer node =
        SmartCardServiceProvider.getService()
            .getPlugin(CardConfigurator.REMOTE_PLUGIN_NAME)
            .getExtension(RemotePluginServer.class)
            .getSyncNode();

    // Forwards the message to the node and returns the response to the client.
    return node.onRequest(message);
  }

  /**
   * Return the SAM status.
   *
   * @return {isSamReady:true} if sam is ready.
   */
  @GET
  @Path("/sam-status")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSamStatus() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("isSamReady", cardSamObserver.isSamAvailable());
    return Response.ok(jsonObject.toString()).build();
  }

  /**
   * Reads the message sent to the remote plugin endpoint.
   *
   * @param body The request content.
   * @return A not null message.
   * @throws BadRequestException If the content is not a valid JSON {@link MessageDto}.
   */
  private MessageDto readMessage(String body) {
    MessageDto message;
    try {
      message = jsonb.fromJson(body, MessageDto.class);
    } catch (JsonbException e) {
      throw badRequest("Invalid message: " + e.getMessage());
    }
    if (message == null) {
      throw badRequest("Invalid message: no content");
    }
    return message;
  }

  private static BadRequestException badRequest(String reason) {
    return new BadRequestException(
        Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.TEXT_PLAIN)
            .entity(reason)
            .build());
  }
}
