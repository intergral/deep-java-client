/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.api.resource;

public class ResourceAttributes {

  /**
   * Logical name of the service.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>MUST be the same for all instances of horizontally scaled services. If the value was not
   *       specified, SDKs MUST fallback to {@code unknown_service:} concatenated with <a
   *       href="process.md#process">{@code process.executable.name}</a>, e.g. {@code
   *       unknown_service:bash}. If {@code process.executable.name} is not available, the value
   *       MUST be set to {@code unknown_service}.
   * </ul>
   */
  public static final String SERVICE_NAME = "service.name";

  /**
   * The name of the telemetry SDK as defined above.
   */
  public static final String TELEMETRY_SDK_NAME = "telemetry.sdk.name";

  /**
   * The language of the telemetry SDK.
   */
  public static final String TELEMETRY_SDK_LANGUAGE = "telemetry.sdk.language";

  /**
   * The version string of the telemetry SDK.
   */
  public static final String TELEMETRY_SDK_VERSION = "telemetry.sdk.version";
}
