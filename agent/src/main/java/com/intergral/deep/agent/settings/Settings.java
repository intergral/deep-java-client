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

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Settings implements ISettings {

  private static final AtomicBoolean IS_ACTIVE = new AtomicBoolean(true);
  private final Properties properties;
  private Resource resource;
  private Collection<IPlugin> plugins = Collections.emptyList();
  private final Collection<IPlugin> customPlugins = new ArrayList<>();

  private Settings(Properties properties) {
    this.properties = properties;
  }

  public static Settings build(final Map<String, String> agentArgs) {
    final String settingFile = readProperty("deep.settings", agentArgs);
    final InputStream propertiesStream;
    if (settingFile != null) {
      try {
        propertiesStream = new FileInputStream(settingFile);
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      propertiesStream = Settings.class.getResourceAsStream("/deep_settings.properties");
    }

    // we have special handling for is active as it is the only value that we allow to change during run time.
    final String isActive = readProperty(ISettings.KEY_ENABLED, agentArgs);
    if (isActive != null && !Boolean.parseBoolean(isActive)) {
      IS_ACTIVE.set(false);
    }

    return build(agentArgs, propertiesStream);
  }

  static Settings build(final Map<String, String> agentArgs, final InputStream stream) {
    final Properties properties = new Properties();
    try (InputStream resourceAsStream = stream) {
      properties.load(resourceAsStream);
    } catch (IOException e) {
      // logging is not initialized until after the settings class
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }

    for (final Map.Entry<Object, Object> propEntry : properties.entrySet()) {
      final String key = String.valueOf(propEntry.getKey());
      final String property = readProperty(key, agentArgs);
      if (property != null) {
        properties.put(key, property);
      }
    }

    properties.putAll(agentArgs);

    return new Settings(properties);
  }

  private static String readProperty(final String key, final Map<String, String> agentArgs) {
    // todo should this not be env over everything else?
    // arguments sent to agent have priority
    final String agentArg = agentArgs.get(key);
    if (agentArg != null) {
      return agentArg;
    }
    // then use env properties
    final String s = readEnvProperty(key);
    if (s != null) {
      return s;
    }
    // then system properties
    return readSystemProperty(key);
  }

  private static String readEnvProperty(final String key) {
    return System.getenv("DEEP_" + key.toUpperCase().replaceAll("\\.", "_"));
  }

  private static String readSystemProperty(final String key) {
    return System.getProperty("deep." + key);
  }

  @SuppressWarnings("unchecked")
  public static <T> T coerc(final String str, final Class<T> type) {
    if (str == null) {
      return null;
    }

    if (type == Boolean.class || type == boolean.class) {
      return (T) Boolean.valueOf(str);
    } else if (type == Integer.class || type == int.class) {
      return (T) Integer.valueOf(Double.valueOf(str).intValue());
    } else if (type == Long.class || type == long.class) {
      return (T) Long.valueOf(Double.valueOf(str).longValue());
    } else if (type == String.class) {
      return (T) str;
    } else if (type == Double.class || type == double.class) {
      return (T) Double.valueOf(str);
    } else if (type == Float.class || type == float.class) {
      return (T) Float.valueOf(str);
    } else if (type == List.class || type == Collection.class) {
      // Java doesnt allow us to know what type of List, so they can only be strings
      return (T) makeList(str);
    } else if (type == Map.class) {
      final List<String> strs = makeList(str);
      final Map<String, String> map = new HashMap<>();

      for (final String s : strs) {
        final String[] split = s.split("=");
        if (split.length == 2) {
          map.put(split[0], split[1]);
        }
      }

      return (T) map;
    } else if (type == Level.class) {
      if (str.equalsIgnoreCase("debug")) {
        return (T) Level.FINEST;
      }
      return (T) Level.parse(str);
    } else if (type == Pattern.class) {
      return (T) Pattern.compile(str);
    } else if (type == URL.class) {
      try {
        return (T) new URL(str);
      } catch (MalformedURLException mue) {
        throw new RuntimeException(str + " is not a valid URL");
      }
    }

    throw new IllegalArgumentException("Cannot coerc " + str + " to " + type);
  }

  private static List<String> makeList(final String str) {
    final String trimmed = str.trim();
    if (trimmed.isEmpty()) {
      return Collections.emptyList();
    }

    String[] split = trimmed.split(",");
    // Either 1 key only or using different format
    if (split.length == 1) {
      split = trimmed.split(";");
    }

    return Arrays.asList(split);
  }

  public <T> T getSettingAs(String key, Class<T> clazz) {
    // special handling for enabled key
    if(key.equals(ISettings.KEY_ENABLED)){
      return coerc(String.valueOf(isActive()), clazz);
    }
    final String property = this.properties.getProperty(key);

    if (property != null) {
      return coerc(property, clazz);
    }

    final String envProp = readEnvProperty(key);
    if (envProp != null) {
      return coerc(envProp, clazz);
    }

    final String systemProperty = readSystemProperty(key);
    if (systemProperty == null) {
      return null;
    }
    return coerc(systemProperty, clazz);
  }

  @Override
  public Map<String, String> getMap(String key) {
    final Map settingAs = getSettingAs(key, Map.class);
    if (settingAs == null) {
      return Collections.emptyMap();
    }
    return settingAs;
  }

  public String getServiceHost() {
    final String serviceUrl = getSettingAs(ISettings.KEY_SERVICE_URL, String.class);
    if (serviceUrl != null && serviceUrl.contains("://")) {
      try {
        return new URL(serviceUrl).getHost();
      } catch (MalformedURLException e) {
        throw new InvalidConfigException(ISettings.KEY_SERVICE_URL, serviceUrl, e);
      }
    } else if (serviceUrl != null && serviceUrl.contains(":")) {
      return serviceUrl.split(":")[0];
    }

    throw new InvalidConfigException(ISettings.KEY_SERVICE_URL, serviceUrl);
  }

  public int getServicePort() {
    final String serviceUrl = getSettingAs(ISettings.KEY_SERVICE_URL, String.class);
    if (serviceUrl != null && serviceUrl.contains("://")) {
      try {
        return new URL(serviceUrl).getPort();
      } catch (MalformedURLException e) {
        throw new InvalidConfigException(ISettings.KEY_SERVICE_URL, serviceUrl, e);
      }
    } else if (serviceUrl != null && serviceUrl.contains(":")) {
      return Integer.parseInt(serviceUrl.split(":")[1]);
    }

    throw new InvalidConfigException(ISettings.KEY_SERVICE_URL, serviceUrl);
  }

  @Override
  public Resource getResource() {
    return this.resource;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public List<String> getAsList(final String key) {
    final List settingAs = getSettingAs(key, List.class);
    if (settingAs == null) {
      return Collections.emptyList();
    }
    return settingAs;
  }

  public Collection<IPlugin> getPlugins() {
    final ArrayList<IPlugin> actualPlugins = new ArrayList<>(this.plugins);
    actualPlugins.addAll(this.customPlugins);
    return actualPlugins;
  }

  public void setPlugins(Collection<IPlugin> plugins) {
    this.plugins = plugins;
  }

  public void addPlugin(final IPlugin plugin) {
    final Optional<IPlugin> first = this.customPlugins.stream().filter(iPlugin -> iPlugin.name().equals(plugin.name())).findFirst();
    if (first.isPresent()) {
      throw new IllegalStateException(String.format("Cannot add duplicate named (%s) plugin", plugin.name()));
    }
    this.customPlugins.add(plugin);
  }

  public void removePlugin(final IPlugin plugin) {
    this.customPlugins.removeIf(iPlugin -> iPlugin.name().equals(plugin.name()));
  }

  @Override
  public IPlugin getPlugin(final String name) {
    final Collection<IPlugin> allPlugins = this.getPlugins();
    for (IPlugin plugin : allPlugins) {
      if (plugin.name().equals(name) || plugin.getClass().getName().equals(name)) {
        return plugin;
      }
    }
    return null;
  }

  public boolean isActive() {
    return IS_ACTIVE.get();
  }

  public void setActive(boolean state) {
    IS_ACTIVE.set(state);
  }

  public static class InvalidConfigException extends RuntimeException {

    public InvalidConfigException(String key, String value) {
      super(String.format("Config value (%s) for key (%s) is invalid.", key, value));
    }

    public InvalidConfigException(String key, String value, Throwable cause) {
      super(String.format("Config value (%s) for key (%s) is invalid.", key, value), cause);
    }
  }
}
