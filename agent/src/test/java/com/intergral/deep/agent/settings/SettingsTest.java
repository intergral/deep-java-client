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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.IRegistration;
import com.intergral.deep.agent.api.logger.ITracepointLogger;
import com.intergral.deep.agent.api.plugin.IMetricProcessor;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.settings.Settings.InvalidConfigException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
  void setActive() {

    final Settings settings = Settings.build(new HashMap<>());

    assertTrue(settings.isActive());
    settings.setActive(false);
    assertFalse(settings.isActive());
  }

  @Test
  void tracepointLogger_null() {
    final Settings settings = Settings.build(new HashMap<>());
    assertNull(settings.getTracepointLogger());
  }

  @Test
  void tracepointLogger_can_log() {
    final Settings settings = Settings.build(new HashMap<>());
    abstract class TPLogger implements IDeepPlugin, ITracepointLogger {

    }
    final TPLogger tracepointLogger = Mockito.mock(TPLogger.class);
    settings.setPlugins(Collections.singletonList(tracepointLogger));
    assertNotNull(settings.getTracepointLogger());

    settings.logTracepoint("log", "tp_id", "snap_id");
    Mockito.verify(tracepointLogger, Mockito.times(1)).logTracepoint("log", "tp_id", "snap_id");
  }

  @Test
  void plugins() {
    final Settings settings = Settings.build(new HashMap<>());
    final TestPlugin plugin = new TestPlugin();
    settings.setPlugins(Collections.singleton(plugin));

    final Collection<IDeepPlugin> plugins = settings.getPlugins();
    assertEquals(1, plugins.size());
    assertEquals(plugin, plugins.iterator().next());

    final Collection<ITracepointLogger> loggers = settings.getPlugins(ITracepointLogger.class);
    assertEquals(1, loggers.size());
    assertEquals(plugin, loggers.iterator().next());

    final ITracepointLogger pluginByName = settings.getPluginByName(ITracepointLogger.class, TestPlugin.class.getName());
    assertNotNull(pluginByName);
    assertSame(pluginByName, plugin);

    final Collection<String> notPlugin = settings.getPlugins(String.class);
    assertTrue(notPlugin.isEmpty());

    final ITracepointLogger notFound = settings.getPluginByName(ITracepointLogger.class, getClass().getName());
    assertNull(notFound);
  }

  @Test
  void addPlugin() {
    final Settings settings = Settings.build(new HashMap<>());

    final TestPlugin plugin = new TestPlugin();
    final IRegistration<IDeepPlugin> iDeepPluginIRegistration = settings.addPlugin(plugin);

    assertNotNull(iDeepPluginIRegistration.get());

    assertSame(plugin, iDeepPluginIRegistration.get());

    assertEquals(1, settings.getPlugins().size());

    assertSame(plugin, settings.getPlugin(TestPlugin.class));

    assertNotNull(settings.getPluginByName(TestPlugin.class, TestPlugin.class.getName()));

    iDeepPluginIRegistration.unregister();

    assertEquals(0, settings.getPlugins().size());

    assertNull(settings.getPluginByName(TestPlugin.class, TestPlugin.class.getName()));

    final IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, iDeepPluginIRegistration::unregister);
    assertEquals("cannot remove plugin: TestPlugin{}", illegalStateException.getMessage());

  }

  private static class TestPlugin implements IDeepPlugin, IMetricProcessor, ITracepointLogger {

    @Override
    public void counter(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
        final Double value) {

    }

    @Override
    public void gauge(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
        final Double value) {

    }

    @Override
    public void histogram(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
        final Double value) {

    }

    @Override
    public void summary(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
        final Double value) {

    }

    @Override
    public void logTracepoint(final String logMsg, final String tracepointId, final String snapshotId) {

    }

    @Override
    public String toString() {
      return "TestPlugin{}";
    }
  }
}