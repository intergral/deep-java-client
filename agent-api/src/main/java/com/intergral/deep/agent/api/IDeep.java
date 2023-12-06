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

package com.intergral.deep.agent.api;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.IPlugin.IPluginRegistration;
import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.api.tracepoint.ITracepoint.ITracepointRegistration;
import java.util.Collection;
import java.util.Map;

/**
 * This type describes the main API for Deep.
 * <p>
 * This API can only be used after the agent has been loaded.
 */
public interface IDeep {

  /**
   * Get the current state of deep.
   *
   * @return {@code true} if deep is currently enabled and sending requests, else {@code false}
   */
  boolean isEnabled();


  /**
   * This method can be used to disabled or enable Deep.
   * <p>
   * Changing the state to {@code false} (ie disabled) will cause deep to uninstall all the tracepoints and clear the current config.
   * Meaning that when deep is enabled again it will have to reinstall the configuration. It is therefore advised to not call this function
   * too frequently.
   *
   * @param enabled the new state to become
   */
  void setEnabled(final boolean enabled);

  /**
   * Get the version of deep being used.
   *
   * @return the sematic version of deep as a string e.g. 1.2.3
   */
  String getVersion();

  /**
   * This allows the registration of custom plugins.
   *
   * @param plugin the plugin that can be used to decorate snapshots
   * @return a {@link IPluginRegistration} that can be used to unregister the plugin
   */
  IPluginRegistration registerPlugin(final IPlugin plugin);

  /**
   * Create a tracepoint that will only exist on this instance.
   *
   * @param path the path to the file
   * @param line the line number
   * @return a {@link ITracepointRegistration} that can be used to remove the tracepoint
   */
  ITracepointRegistration registerTracepoint(final String path, final int line);

  /**
   * Create a tracepoint that will only exist on this instance.
   *
   * @param path the path to the file
   * @param line the line number
   * @param args the key value pairs that further define the tracepoint
   * @param watches the list of watch expressions
   * @param metrics the list of metric expressions
   * @return a {@link ITracepointRegistration} that can be used to remove the tracepoint
   */
  ITracepointRegistration registerTracepoint(final String path, final int line, final Map<String, String> args,
      final Collection<String> watches, final Collection<MetricDefinition> metrics);
}
