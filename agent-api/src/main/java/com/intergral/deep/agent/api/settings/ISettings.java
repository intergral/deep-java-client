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

package com.intergral.deep.agent.api.settings;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.resource.Resource;
import java.util.List;
import java.util.Map;

/**
 * The exposed API for the Deep settings.
 */
public interface ISettings {

  /**
   * This is the settings key for the configured auth provider.
   */
  String KEY_AUTH_PROVIDER = "service.auth.provider";

  /**
   * This is the setting key for enabling or disabling deep.
   */
  String KEY_ENABLED = "enabled";

  /**
   * This is the setting key for the service url.
   */
  String KEY_SERVICE_URL = "service.url";

  /**
   * This is the setting key for the service secure setting.
   */
  String KEY_SERVICE_SECURE = "service.secure";

  /**
   * This is the setting key for the plugin list.
   */
  String PLUGINS = "plugins";

  /**
   * To let us calculate the class and file names for JSP classes we need to know the JSP suffix that is being used by monitored service.
   * <p>
   * By default, tomcat take index.jsp and make it into index_jsp.class, but this suffix can be configured.
   */
  String JSP_SUFFIX = "jsp.suffix";

  /**
   * It is possible to put compiled JSP classes into specified packages, some versions put this in a {@code jsp} package, some use
   * {@code org.apache.jsp} (newer).
   */
  String JSP_PACKAGES = "jsp.packages";

  /**
   * Define which packages we should include as being part of your app.
   */
  String APP_FRAMES_INCLUDES = "in.app.include";

  /**
   * Define which packages we should exclude as being part of your app.
   */
  String APP_FRAMES_EXCLUDES = "in.app.include";

  /**
   * Get a setting from the config as a given type.
   * <p>
   * When reading a config value, deep will scan the config file, system properties and environment variables. We will read the
   * values in order:
   * <ul>
   *  <li>Environment Variables</li>
   *  <li>System Property</li>
   *  <li>Config Value</li>
   * </ul>
   *
   * So a value read from environment variables will override values given as system properties.
   * <p>
   * When we read a value we will scope the keys to suit the place from which we are reading. This means that if we
   * are looking for 'service.name' we will use the following keys:
   * <ul>
   *   <li>DEEP_SERVICE_NAME - When looking in Environment variables we uppercase and use underscores</li>
   *   <li>deep.service.name - When looking in System Properties we lowercase and use dots</li>
   *   <li>service.name - When looking in config file we use the normalised value</li>
   * </ul>
   *
   * The key provided should be the normalized key.
   *
   * @param key   the key for the setting
   * @param clazz the type to return as
   * @param <T>   the type to return as
   * @return the value as the given type
   */
  <T> T getSettingAs(String key, Class<T> clazz);

  /**
   * Get the property as a map.
   *
   * @param key the for the setting
   * @return the value as a map
   */
  Map<String, String> getMap(String key);

  /**
   * Get the property as a list.
   *
   * @param key for the setting
   * @return the value as a list
   */
  List<String> getAsList(String key);

  /**
   * Returns the resource that describes this client.
   *
   * @return the {@link Resource}
   */
  Resource getResource();

  /**
   * Look for a plugin with the given name or class name.
   *
   * @param name the plugin name or the plugin class name
   * @return the {@link IPlugin} or {@code null}
   */
  IPlugin getPlugin(final String name);
}
