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

using Newtonsoft.Json;

namespace App.domain.data.executeremoteservice
{
    /// <summary>
    /// Input data of the remote service SELECT_APP_AND_LOAD_TRIPS: serial number of the card read
    /// before, the trips being only loaded in this card, and number of trips added to the multi-trip
    /// ticket, as a decimal integer (JSON number).
    /// </summary>
    class LoadTripsInputData : InputData
    {
        [JsonProperty("applicationSerialNumber")]
        public required string ApplicationSerialNumber { get; set; }

        [JsonProperty("tripsToLoad")]
        public required int TripsToLoad { get; set; }
    }
}
