// Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
//
// See the NOTICE file(s) distributed with this work for additional information
// regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the BSD 3-Clause License which is available at
// https://opensource.org/licenses/BSD-3-Clause.
//
// SPDX-License-Identifier: BSD-3-Clause

namespace App.domain.api
{
    /// <summary>
    /// Defines the operations that the ticketing service API must support. 
    /// These operations primarily involve interacting with a card, where the card transaction
    /// is driven by the server provided to the implementation of this interface.
    /// </summary>
    public interface TicketingServiceApi
    {

        /// <summary>
        /// Blocks until a card is inserted. 
        /// </summary>
        void WaitForCard();

        /// <summary>
        /// Selects the application of the card and reads its contracts.
        /// The transaction is fully operated by the server.
        /// </summary>
        /// <returns>A string representation of the contracts read from the card.</returns>
        string SelectAppAndReadContracts();

        /// <summary>
        /// Selects the application of the card and loads trips onto its multi-trip ticket.
        /// The transaction is fully operated by the server.
        /// </summary>
        /// <param name="tripsToLoad">The number of trips to load.</param>
        /// <returns>A string representation of the status of the operation.</returns>
        string SelectAppAndLoadTrips(int tripsToLoad);
    }
}
