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

package com.intergral.deep.agent.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intergral.deep.agent.ReflectionUtils;
import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.plugin.JavaPlugin;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class PluginLoaderTest {

  @Test
  void canLoadNoPlugins() {
    final List<IPlugin> iPlugins = PluginLoader.loadPlugins(Settings.build(Collections.emptyMap()), ReflectionUtils.getReflection());
    assertEquals(1, iPlugins.size());
    assertEquals(iPlugins.get(0).name(), JavaPlugin.class.getName());
  }

  @Test
  void canLoadBadPlugin() {
    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put(ISettings.PLUGINS, BadPlugin.class.getName());
    final List<IPlugin> iPlugins = PluginLoader.loadPlugins(Settings.build(agentArgs), ReflectionUtils.getReflection());
    assertEquals(0, iPlugins.size());
  }

  @Test
  void canLoadGoodPlugin() {
    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put(ISettings.PLUGINS, GoodPlugin.class.getName());
    final List<IPlugin> iPlugins = PluginLoader.loadPlugins(Settings.build(agentArgs), ReflectionUtils.getReflection());
    assertEquals(1, iPlugins.size());
    assertEquals(iPlugins.get(0).name(), GoodPlugin.class.getName());
  }

  public static class BadPlugin {

  }

  public static class GoodPlugin implements IPlugin {

    @Override
    public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
      return null;
    }
  }
}