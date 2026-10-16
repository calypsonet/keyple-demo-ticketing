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

/// <summary>
/// MessageDto is a data transfer object for representing messages exchanged with the Keyple ticketing server.
/// </summary>
public class MessageDto
{
    /// <summary>
    /// API level.
    /// </summary>
    [JsonProperty("apiLevel")]
    public required int ApiLevel { get; set; }

    /// <summary>
    /// Session ID.
    /// </summary>
    [JsonProperty("sessionId")]
    public required string SessionId { get; set; }

    /// <summary>
    /// Action associated with the message.
    /// </summary>
    [JsonProperty("action")]
    public required string Action { get; set; }

    /// <summary>
    /// Client node ID.
    /// </summary>
    [JsonProperty("clientNodeId")]
    public required string ClientNodeId { get; set; }

    /// <summary>
    /// Server node ID.
    /// </summary>
    [JsonProperty("serverNodeId")]
    public string? ServerNodeId { get; set; }

    /// <summary>
    /// Name of the local reader.
    /// </summary>
    [JsonProperty("localReaderName")]
    public string? LocalReaderName { get; set; }

    /// <summary>
    /// Name of the remote reader.
    /// </summary>
    [JsonProperty("remoteReaderName")]
    public string? RemoteReaderName { get; set; }

    /// <summary>
    /// Body of the message.
    /// </summary>
    [JsonProperty("body")]
    public required string Body { get; set; }
}
