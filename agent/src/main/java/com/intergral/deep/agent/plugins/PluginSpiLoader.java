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

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IConditional;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.resource.ResourceDetector;
import com.intergral.deep.agent.resource.SpiUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This acts as the main loader for plugins using the SPI loader system.
 */
public class PluginSpiLoader {

  // Visible for testing
  static final String ENABLED_PLUGIN_KEY = "deep.java.enabled.plugin";
  static final String DISABLED_PLUGIN_KEY = "deep.java.disabled.plugin";

  /**
   * Load all available plugins.
   *
   * @param settings   the current settings
   * @param reflection the reflection service
   * @param loader     the classloader to use
   * @return the list of loaded plugins in order
   * @see IDeepPlugin
   */
  public static List<IDeepPlugin> loadPlugins(final ISettings settings, final IReflection reflection, final ClassLoader loader) {
    Set<String> enabledProviders =
        new HashSet<>(settings.getAsList(ENABLED_PLUGIN_KEY));
    Set<String> disabledProviders =
        new HashSet<>(settings.getAsList(DISABLED_PLUGIN_KEY));
    final List<IDeepPlugin> iDeepPlugins = SpiUtil.loadOrdered(IDeepPlugin.class, loader);
    return iDeepPlugins.stream()
        .filter(plugin -> !ResourceDetector.isDisabled(plugin.getClass(), enabledProviders, disabledProviders))
        .filter(plugin -> {
          if (plugin instanceof IConditional) {
            return ((IConditional) plugin).isActive();
          }
          return true;
        })
        .map(plugin -> plugin.configure(settings, reflection))
        .collect(Collectors.toList());
  }
}
