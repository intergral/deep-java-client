/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.api.resource;

/**
 * An exception that is thrown if the user-provided configuration is invalid.
 */
public final class ConfigurationException extends RuntimeException {

  /**
   * Create a new configuration exception with specified {@code message} and without a cause.
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Create a new configuration exception with specified {@code message} and {@code cause}.
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
