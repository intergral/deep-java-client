/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.api.resource;


import static com.intergral.deep.agent.api.resource.ResourceAttributes.SERVICE_NAME;
import static com.intergral.deep.agent.api.resource.ResourceAttributes.TELEMETRY_SDK_LANGUAGE;
import static com.intergral.deep.agent.api.resource.ResourceAttributes.TELEMETRY_SDK_NAME;
import static com.intergral.deep.agent.api.resource.ResourceAttributes.TELEMETRY_SDK_VERSION;

import com.intergral.deep.agent.api.DeepVersion;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * {@link Resource} represents a resource, which capture identifying information about the entities for which signals (stats or traces) are
 * reported.
 */
public class Resource {

  public static final Resource DEFAULT;
  private static final Logger logger = Logger.getLogger(Resource.class.getName());
  private static final Resource EMPTY = create(Collections.emptyMap());
  private static final Resource TELEMETRY_SDK;
  /**
   * The MANDATORY Resource instance contains the mandatory attributes that must be used if they are not provided by the Resource that is
   * given to an SDK signal provider.
   */
  private static final Resource MANDATORY =
      create(Collections.singletonMap(SERVICE_NAME, "unknown_service:java"));

  static {
    final HashMap<String, Object> attributeMap = new HashMap<>();
    attributeMap.put(TELEMETRY_SDK_NAME, "deep");
    attributeMap.put(TELEMETRY_SDK_LANGUAGE, "java");
    attributeMap.put(TELEMETRY_SDK_VERSION, DeepVersion.VERSION);

    TELEMETRY_SDK = create(attributeMap);
    DEFAULT = MANDATORY.merge(TELEMETRY_SDK);
  }

  private final Map<String, Object> attributes;
  private final String schemaUrl;

  private Resource(String schemaUrl, Map<String, Object> attributes) {
    this.schemaUrl = schemaUrl;
    this.attributes = attributes;
  }


  /**
   * Returns a {@link Resource}.
   *
   * @param attributes a map of attributes that describe the resource.
   * @return a {@code Resource}.
   * @throws NullPointerException if {@code attributes} is null.
   */
  public static Resource create(Map<String, Object> attributes) {
    return create(attributes, null);
  }

  /**
   * Returns a {@link Resource}.
   *
   * @param attributes a map of attributes that describe the resource.
   * @param schemaUrl  The URL of the OpenTelemetry schema used to create this Resource.
   * @return a {@code Resource}.
   * @throws NullPointerException if {@code attributes} is null.
   */
  public static Resource create(Map<String, Object> attributes, String schemaUrl) {
    return new Resource(schemaUrl, attributes);
  }


  /**
   * Returns the URL of the OpenTelemetry schema used by this resource. May be null.
   *
   * @return An OpenTelemetry schema URL.
   * @since 1.4.0
   */
  public String getSchemaUrl() {
    return this.schemaUrl;
  }

  /**
   * Returns a map of attributes that describe the resource.
   *
   * @return a map of attributes.
   */
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }


  /**
   * Returns a new, merged {@link Resource} by merging the current {@code Resource} with the {@code other} {@code Resource}. In case of a
   * collision, the "other" {@code Resource} takes precedence.
   *
   * @param other the {@code Resource} that will be merged with {@code this}.
   * @return the newly merged {@code Resource}.
   */
  public Resource merge(Resource other) {
    if (other == null || other == EMPTY) {
      return this;
    }

    final HashMap<String, Object> hashMap = new HashMap<>(this.getAttributes());
    hashMap.putAll(other.getAttributes());

    if (other.getSchemaUrl() == null) {
      return create(hashMap, getSchemaUrl());
    }
    if (getSchemaUrl() == null) {
      return create(hashMap, other.getSchemaUrl());
    }
    if (!other.getSchemaUrl().equals(getSchemaUrl())) {
      logger.info(
          "Attempting to merge Resources with different schemaUrls. "
              + "The resulting Resource will have no schemaUrl assigned. Schema 1: "
              + getSchemaUrl()
              + " Schema 2: "
              + other.getSchemaUrl());
      // currently, behavior is undefined if schema URLs don't match. In the future, we may
      // apply schema transformations if possible.
      return create(hashMap, null);
    }
    return create(hashMap, getSchemaUrl());
  }
}
