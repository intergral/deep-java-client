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

import com.intergral.deep.agent.api.DeepRuntimeException;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.resource.ResourceAttributes;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.ResourceProvider;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * This provider will read values from the environment to configure a resource.
 * <p>
 * The values this will look for are:
 * <ul>
 *  <li>- service.name - the value to use as the service name</li>
 *  <li>- resource.attributes - a list of key value pairs to read as attributes</li>
 * </ul>
 * The {@code resource.attributes} should follow the patterns defined by <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/sdk.md#specifying-resource-information-via-an-environment-variable">open telemetry</a>
 * <p>
 * The values will be read as either values in the config, system properties or environment variables.
 *
 * @see ISettings#getSettingAs(String, Class)
 */
public class EnvironmentResourceProvider implements ResourceProvider {

  static final String SERVICE_NAME_PROPERTY = "service.name";
  static final String ATTRIBUTE_PROPERTY = "resource.attributes";

  // visible for testing
  static Map<String, Object> getAttributes(ISettings settings) {
    Map<String, Object> resourceAttributes = new HashMap<>();
    try {
      final Map<String, String> attributes = settings.getMap(ATTRIBUTE_PROPERTY);
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
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

  @Override
  public Resource createResource(final ISettings settings) {
    return Resource.create(getAttributes(settings), null);
  }
}
