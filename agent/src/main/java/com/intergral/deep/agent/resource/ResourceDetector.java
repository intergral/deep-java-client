/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.spi.ConditionalResourceProvider;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.api.spi.ResourceProvider;
import com.intergral.deep.agent.settings.Settings;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities to create the resource for this agent.
 */
public final class ResourceDetector {

  private ResourceDetector() {
  }

  // Visible for testing
  static final String DISABLED_ATTRIBUTE_KEYS = "resource.disabled.keys";
  static final String ENABLED_PROVIDERS_KEY = "java.enabled.resource.providers";
  static final String DISABLED_PROVIDERS_KEY = "java.disabled.resource.providers";

  /**
   * Create and configure a resource for this agent.
   *
   * @param settings the settings for the agent
   * @param plugins  the list of discovered plugins
   * @return the loaded resource
   */
  public static Resource configureResource(final Settings settings, final List<IDeepPlugin> plugins) {
    Resource result = Resource.create(Collections.emptyMap());

    Set<String> enabledProviders =
        new HashSet<>(settings.getAsList(ENABLED_PROVIDERS_KEY));
    Set<String> disabledProviders =
        new HashSet<>(settings.getAsList(DISABLED_PROVIDERS_KEY));

    for (IDeepPlugin plugin : plugins) {
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
    if (isDisabled(resourceProvider.getClass(), enabledProviders, disabledProviders)) {
      return null;
    }

    if (resourceProvider instanceof ConditionalResourceProvider
        && !((ConditionalResourceProvider) resourceProvider).shouldApply(settings, result)) {
      return null;
    }
    return result.merge(resourceProvider.createResource(settings));
  }

  public static boolean isDisabled(final Class<?> providerClass, final Set<String> enabledProviders, final Set<String> disabledProviders) {
    if (!enabledProviders.isEmpty()
        && !enabledProviders.contains(providerClass.getName())) {
      return true;
    }
    return disabledProviders.contains(providerClass.getName());
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
