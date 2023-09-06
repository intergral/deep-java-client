/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.spi.ConditionalResourceProvider;
import com.intergral.deep.agent.api.spi.ResourceProvider;
import com.intergral.deep.agent.settings.Settings;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities to create the resource for this agent.
 */
public final class ResourceDetector {

  private ResourceDetector() {
  }

  // Visible for testing
  static final String DISABLED_ATTRIBUTE_KEYS = "deep.resource.disabled.keys";
  static final String ENABLED_PROVIDERS_KEY = "deep.java.enabled.resource.providers";
  static final String DISABLED_PROVIDERS_KEY = "deep.java.disabled.resource.providers";

  /**
   * Create and configure a resource for this agent.
   *
   * @param settings           the settings for the agent
   * @param serviceClassLoader the class loader to use to load the SPI services
   * @return the loaded resource
   */
  public static Resource configureResource(Settings settings, ClassLoader serviceClassLoader) {
    Resource result = Resource.create(Collections.emptyMap());

    Set<String> enabledProviders =
        new HashSet<>(settings.getAsList(ENABLED_PROVIDERS_KEY));
    Set<String> disabledProviders =
        new HashSet<>(settings.getAsList(DISABLED_PROVIDERS_KEY));

    for (ResourceProvider resourceProvider :
        SpiUtil.loadOrdered(ResourceProvider.class, serviceClassLoader)) {
      final Resource resource = getResource(settings, resourceProvider, enabledProviders, disabledProviders, result);
      if (resource != null) {
        result = resource;
      }
    }

    // let plugins also act as resource providers
    for (IPlugin plugin : settings.getPlugins()) {
      if (!(plugin instanceof ResourceProvider)) {
        continue;
      }
      final Resource resource = getResource(settings, (ResourceProvider) plugin, enabledProviders, disabledProviders, result);
      if (resource != null) {
        result = resource;
      }
    }

    result = filterAttributes(result, settings);

    return result;
  }

  private static Resource getResource(final Settings settings, final ResourceProvider resourceProvider, final Set<String> enabledProviders,
      final Set<String> disabledProviders, final Resource result) {
    if (!enabledProviders.isEmpty()
        && !enabledProviders.contains(resourceProvider.getClass().getName())) {
      return null;
    }
    if (disabledProviders.contains(resourceProvider.getClass().getName())) {
      return null;
    }
    if (resourceProvider instanceof ConditionalResourceProvider
        && !((ConditionalResourceProvider) resourceProvider).shouldApply(settings, result)) {
      return null;
    }
    return result.merge(resourceProvider.createResource(settings));
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
