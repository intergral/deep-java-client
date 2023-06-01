/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.resource.ConfigurationException;
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

  public static Resource configureResource(
      Settings config,
      ClassLoader serviceClassLoader) {
    Resource result = Resource.create(Collections.emptyMap());

    Set<String> enabledProviders =
        new HashSet<>(config.getAsList("deep.java.enabled.resource.providers"));
    Set<String> disabledProviders =
        new HashSet<>(config.getAsList("deep.java.disabled.resource.providers"));

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
          && !((ConditionalResourceProvider) resourceProvider).shouldApply(config, result)) {
        continue;
      }
      result = result.merge(resourceProvider.createResource(config));
    }

    result = result.merge(createEnvironmentResource(config));

    result = filterAttributes(result, config);

    return result;
  }

  private static Resource createEnvironmentResource(Settings config) {
    return Resource.create(getAttributes(config), null);
  }

  // visible for testing
  static Map<String, Object> getAttributes(Settings configProperties) {
    Map<String, Object> resourceAttributes = new HashMap<>();
    try {
      for (Map.Entry<String, String> entry :
          configProperties.getMap(ATTRIBUTE_PROPERTY).entrySet()) {
        resourceAttributes.put(
            entry.getKey(),
            // Attributes specified via deep.resource.attributes follow the W3C Baggage spec and
            // characters outside the baggage-octet range are percent encoded
            // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/sdk.md#specifying-resource-information-via-an-environment-variable
            URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8.displayName()));
      }
    } catch (UnsupportedEncodingException e) {
      // Should not happen since always using standard charset
      throw new ConfigurationException("Unable to decode resource attributes.", e);
    }
    String serviceName = configProperties.getSettingAs(SERVICE_NAME_PROPERTY, String.class);
    if (serviceName != null) {
      resourceAttributes.put(ResourceAttributes.SERVICE_NAME, serviceName);
    }
    return resourceAttributes;
  }

  // visible for testing
  static Resource filterAttributes(Resource resource, Settings configProperties) {
    Set<String> disabledKeys = new HashSet<>(configProperties.getAsList(
        DISABLED_ATTRIBUTE_KEYS));

    final Map<String, Object> attributes = resource.getAttributes();
    for (String disabledKey : disabledKeys) {
      attributes.remove(disabledKey);
    }

    return Resource.create(attributes, resource.getSchemaUrl());
  }
}
