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

package com.intergral.deep.agent.api.plugin;

import com.intergral.deep.agent.api.IRegistration;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;

/**
 * This type is used to define a Deep Plugin.
 * <p>
 * Plugins can decorate the snapshot when they are captured allowing for additional metadata to be attached. e.g. OTEL span data.
 */
public interface IPlugin {

  /**
   * This method is called by Deep after a snapshot is created.
   * <p>
   * This method is executed inline with the tracepoint code.
   *
   * @param settings the current settings of Deep
   * @param snapshot the {@link ISnapshotContext} describing the snapshot
   * @return a new {@link Resource} to be added to the snapshot, or {@code null} to do nothing
   */
  Resource decorate(final ISettings settings, final ISnapshotContext snapshot);

  /**
   * The name of the plugin. This should be unique.
   *
   * @return the plugin name, defaults to the full class name
   */
  default String name() {
    return this.getClass().getName();
  }

  /**
   * Is this plugin active.
   * <p>
   * By default, this will check the Deep settings for the plugin name. e.g. the setting com.intergral.deep.plugin.JavaPlugin.active=false
   * will disable the JavaPlugin. The normal settings rules apply, e.g. deep. or DEEP_ as a prefix when using system properties
   * or environment variables.
   *
   * @param settings the current deep settings.
   * @return {@code false} if setting is 'false', otherwise {@code true}
   */
  default boolean isActive(final ISettings settings) {
    final String simpleName = this.name();
    final Boolean settingAs = settings.getSettingAs(String.format("%s.active", simpleName),
        Boolean.class);
    if (settingAs == null) {
      return true;
    }
    return settingAs;
  }

  /**
   * This type describes a registered plugin.
   */
  interface IPluginRegistration extends IRegistration<IPlugin> {

    /**
     * Indicates if this plugin is currently set to be the auth provider.
     *
     * @return {@code true} if the registered plugin is an {@link com.intergral.deep.agent.api.auth.IAuthProvider} and deep is configured
     *     to use this provider, else {@code false}
     */
    boolean isAuthProvider();

    /**
     * Indicates if this plugin is being used to decorate the resource data.
     *
     * @return {@code true} if this plugin was used to decorate the resource data, else {@code false}.
     */
    boolean isResourceProvider();

    /**
     * Indicates if this plugin is being used to log tracepoints.
     *
     * @return {@code true} if this plugin is being used to log the tracepoint logs, else {@code false}.
     */
    boolean isTracepointLogger();
  }
}
