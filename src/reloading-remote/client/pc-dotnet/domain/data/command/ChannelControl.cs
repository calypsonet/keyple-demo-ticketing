﻿// Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
//
// See the NOTICE file(s) distributed with this work for additional information
// regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the BSD 3-Clause License which is available at
// https://opensource.org/licenses/BSD-3-Clause.
//
// SPDX-License-Identifier: BSD-3-Clause

namespace App.domain.data.command
{
    /// <summary>
    /// Channel control mode.
    /// </summary>
    public enum ChannelControl
    {
        /// <summary>
        /// Keep the channel open.
        /// </summary>
        KEEP_OPEN,

        /// <summary>
        /// Close the channel after the operation.
        /// </summary>
        CLOSE_AFTER,
    }
}
