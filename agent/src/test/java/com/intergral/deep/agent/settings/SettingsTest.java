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

package com.intergral.deep.agent.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.settings.Settings.InvalidConfigException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SettingsTest {

  @Test
  void canHandleTypes() {
    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put("string", "string");
    agentArgs.put("number", "1.2");
    agentArgs.put("list", "one,two");
    agentArgs.put("map", "key=one,other=two");
    agentArgs.put("regex", "intergral(s)");
    agentArgs.put("url", "https://google.com");
    agentArgs.put("debug", "DEBUG");
    agentArgs.put("debug_2", "debug");
    agentArgs.put("level", "FINE");

    final Settings settings = Settings.build(agentArgs);

    assertEquals("string", settings.getSettingAs("string", String.class));
    assertEquals(1, settings.getSettingAs("number", int.class));
    assertEquals(1L, settings.getSettingAs("number", long.class));
    assertEquals(1.2f, settings.getSettingAs("number", float.class));
    assertEquals(1.2d, settings.getSettingAs("number", double.class));

    assertEquals("one", settings.getSettingAs("list", List.class).get(0));
    assertEquals("one", settings.getAsList("list").get(0));

    assertEquals("two", settings.getSettingAs("map", Map.class).get("other"));
    assertEquals("two", settings.getMap("map").get("other"));

    assertEquals("intergral(s)", settings.getSettingAs("regex", Pattern.class).pattern());
    assertEquals("https://google.com", settings.getSettingAs("url", URL.class).toString());
    assertEquals(Level.FINEST, settings.getSettingAs("debug", Level.class));
    assertEquals(Level.FINEST, settings.getSettingAs("debug_2", Level.class));
    assertEquals(Level.FINE, settings.getSettingAs("level", Level.class));
  }

  @Test
  void canHandleURLs() {
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "://bad-url"));

      final InvalidConfigException invalidConfigException = assertThrows(InvalidConfigException.class, settings::getServiceHost);
      assertEquals(MalformedURLException.class, invalidConfigException.getCause().getClass());
      assertThrows(InvalidConfigException.class, settings::getServicePort);
    }
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "https://google.com"));

      assertEquals("google.com", settings.getServiceHost());
      assertEquals(-1, settings.getServicePort());
    }
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "http://google.com:80"));

      assertEquals("google.com", settings.getServiceHost());
      assertEquals(80, settings.getServicePort());
    }
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "google.com:443"));

      assertEquals("google.com", settings.getServiceHost());
      assertEquals(443, settings.getServicePort());
    }
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "google.com:80"));

      assertEquals("google.com", settings.getServiceHost());
      assertEquals(80, settings.getServicePort());
    }
    {
      final Settings settings = Settings.build(Collections.singletonMap(Settings.KEY_SERVICE_URL, "google.com"));

      assertThrows(InvalidConfigException.class, settings::getServiceHost);
      assertThrows(InvalidConfigException.class, settings::getServicePort);
    }
  }

  @Test
  void plugins() {

    final Settings settings = Settings.build(new HashMap<>());

    final IPlugin plugin = new IPlugin() {
      @Override
      public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
        return null;
      }
    };
    settings.addPlugin(plugin);
    final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> settings.addPlugin(plugin));
    assertEquals("Cannot add duplicate named (com.intergral.deep.agent.settings.SettingsTest$1) plugin",
        illegalStateException.getMessage());

    assertEquals(1, settings.getPlugins().size());
    settings.removePlugin(plugin);

    assertEquals(0, settings.getPlugins().size());
  }

  @Test
  void setActive() {

    final Settings settings = Settings.build(new HashMap<>());

    assertTrue(settings.isActive());
    settings.setActive(false);
    assertFalse(settings.isActive());
  }
}