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

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.reflection.ReflectionUtils;
import com.intergral.deep.agent.settings.Settings;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this type deals with loading the plugins.
 */
public final class PluginLoader {

  private PluginLoader() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);

  /**
   * Using the current config load as many plugins as we can.
   *
   * @param settings   the settings for deep
   * @param reflection the reflection service to use
   * @return the list of active and loaded plugins
   */
  public static List<IPlugin> loadPlugins(final Settings settings, final IReflection reflection) {
    final List<String> plugins = settings.getAsList("plugins");
    final List<IPlugin> loadedPlugins = new ArrayList<>();
    for (String plugin : plugins) {
      try {
        final Class<?> aClass = Class.forName(plugin);
        final Constructor<?> constructor = ReflectionUtils.findConstructor(aClass, reflection);
        final IPlugin asPlugin = ReflectionUtils.callConstructor(constructor, settings, reflection);
        if (asPlugin.isActive(settings)) {
          loadedPlugins.add(asPlugin);
        }
      } catch (Exception e) {
        LOGGER.error("Cannot load plugin {}", plugin, e);
      }
    }
    return loadedPlugins;
  }

}
