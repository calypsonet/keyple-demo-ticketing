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

import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;

/**
 * Messages of the statuses of the remote services, returned to the clients which display them
 * (multiplatform and .NET clients): texts for the users, as the ones of the Android applications,
 * the causes of the errors being logged by the server.
 */
final class RemoteServiceMessages {

  private RemoteServiceMessages() {}

  /**
   * Returns the message of a status.
   *
   * @param status The status of the remote service.
   * @param calypsoCard The selected card, giving the file structure of a rejected card, null if no
   *     card has been selected.
   * @return A not empty message.
   */
  static String of(RemoteServiceStatus status, CalypsoCard calypsoCard) {
    switch (status) {
      case SUCCESS:
        return "Success";
      case CARD_COMMUNICATION_ERROR:
        return "Card communication error";
      case CARD_REJECTED:
        return calypsoCard == null
            ? "Invalid card"
            : "Invalid card\nFile structure "
                + HexUtil.toHex(calypsoCard.getApplicationSubtype())
                + "h not supported";
      case CARD_NOT_PERSONALIZED:
        return "Environment error: wrong version number";
      case ENVIRONMENT_EXPIRED:
        return "Environment error: end date expired";
      case DIFFERENT_CARD:
        return "Not the same card";
      case CARD_FULL:
        return "No space left on the card for a new ticket";
      case SERVER_ERROR:
      default:
        return "Server error";
    }
  }
}
