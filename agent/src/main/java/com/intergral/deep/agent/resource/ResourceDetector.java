/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.DeepRuntimeException;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.resource.ResourceAttributes;
import com.intergral.deep.agent.api.spi.ConditionalResourceProvider;
import com.intergral.deep.agent.api.spi.ResourceProvider;
import com.intergral.deep.agent.settings.Settings;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceDetector {

  // Visible for testing
  static final String ATTRIBUTE_PROPERTY = "deep.resource.attributes";
  static final String SERVICE_NAME_PROPERTY = "deep.service.name";
  static final String DISABLED_ATTRIBUTE_KEYS = "deep.resource.disabled.keys";
  static final String ENABLED_PROVIDERS_KEY = "deep.java.enabled.resource.providers";
  static final String DISABLED_PROVIDERS_KEY = "deep.java.disabled.resource.providers";

  public static Resource configureResource(Settings settings, ClassLoader serviceClassLoader) {
    Resource result = Resource.create(Collections.emptyMap());

    Set<String> enabledProviders =
        new HashSet<>(settings.getAsList(ENABLED_PROVIDERS_KEY));
    Set<String> disabledProviders =
        new HashSet<>(settings.getAsList(DISABLED_PROVIDERS_KEY));

    for (ResourceProvider resourceProvider :
        SpiUtil.loadOrdered(ResourceProvider.class, serviceClassLoader)) {
      if (!enabledProviders.isEmpty()
          && !enabledProviders.contains(resourceProvider.getClass().getName())) {
        continue;
      }
      if (disabledProviders.contains(resourceProvider.getClass().getName())) {
        continue;
      }
      if (resourceProvider instanceof ConditionalResourceProvider
          && !((ConditionalResourceProvider) resourceProvider).shouldApply(settings, result)) {
        continue;
      }
      result = result.merge(resourceProvider.createResource(settings));
    }

    result = result.merge(createEnvironmentResource(settings));

    result = filterAttributes(result, settings);

    return result;
  }

  private static Resource createEnvironmentResource(Settings settings) {
    return Resource.create(getAttributes(settings), null);
  }

  // visible for testing
  static Map<String, Object> getAttributes(Settings settings) {
    Map<String, Object> resourceAttributes = new HashMap<>();
    try {
      for (Map.Entry<String, String> entry :
          settings.getMap(ATTRIBUTE_PROPERTY).entrySet()) {
        resourceAttributes.put(
            entry.getKey(),
            // Attributes specified via deep.resource.attributes follow the W3C Baggage spec and
            // characters outside the baggage-octet range are percent encoded
            // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/sdk.md#specifying-resource-information-via-an-environment-variable
            URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8.displayName()));
      }
    } catch (UnsupportedEncodingException e) {
      // Should not happen since always using standard charset
      throw new DeepRuntimeException("Unable to decode resource attributes.", e);
    }
    String serviceName = settings.getSettingAs(SERVICE_NAME_PROPERTY, String.class);
    if (serviceName != null) {
      resourceAttributes.put(ResourceAttributes.SERVICE_NAME, serviceName);
    }
    return resourceAttributes;
  }

  // visible for testing
  static Resource filterAttributes(Resource resource, Settings settings) {
    Set<String> disabledKeys = new HashSet<>(settings.getAsList(
        DISABLED_ATTRIBUTE_KEYS));

    final Map<String, Object> attributes = resource.getAttributes();
    for (String disabledKey : disabledKeys) {
      attributes.remove(disabledKey);
    }

    return Resource.create(attributes, resource.getSchemaUrl());
  }
}
