/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.domain

import org.calypsonet.keyple.demo.validation.domain.model.Status

/**
 * Exception for validation business rule violations.
 *
 * Carries the appropriate [org.calypsonet.keyple.demo.validation.domain.model.Status] to set when
 * the exception is caught in the repository layer.
 *
 * @param message The error message describing the validation failure
 * @param status The status to set when this validation exception is caught
 */
class ValidationException(message: String, val status: Status) : RuntimeException(message)
