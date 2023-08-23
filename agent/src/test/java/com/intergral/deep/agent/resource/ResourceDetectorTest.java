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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ResourceDetectorTest {

  @Test
  void loadsResource() {
    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put(ResourceDetector.ENABLED_PROVIDERS_KEY, "");
    agentArgs.put(ResourceDetector.DISABLED_PROVIDERS_KEY, JavaResourceDetector.class.getName());
    final Settings settings = Settings.build(agentArgs);
    final Resource resource = ResourceDetector.configureResource(settings, getClass().getClassLoader());

    assertNotNull(resource);
    assertEquals(0, resource.getAttributes().size());
  }

  @Test
  void loadResourceFromSettings() {
    final Settings settings = Settings.build(new HashMap<>());
    final Resource resource = ResourceDetector.configureResource(settings, getClass().getClassLoader());
    assertEquals(System.getProperty("java.version"), resource.getAttributes().get("java_version"));
  }

  @Test
  void loadResourceFromConfig() {
    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put(ResourceDetector.ATTRIBUTE_PROPERTY, "key=value,other=thing");
    final Settings settings = Settings.build(agentArgs);
    final Resource resource = ResourceDetector.configureResource(settings, getClass().getClassLoader());

    assertEquals("value", resource.getAttributes().get("key"));
    assertEquals("thing", resource.getAttributes().get("other"));
  }
}