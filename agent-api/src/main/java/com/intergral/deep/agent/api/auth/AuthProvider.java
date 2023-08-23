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

package com.intergral.deep.agent.api.auth;

import com.intergral.deep.agent.api.DeepRuntimeException;
import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.reflection.ReflectionUtils;
import com.intergral.deep.agent.api.settings.ISettings;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;

public class AuthProvider {

  private static final NoopProvider NOOP_PROVIDER = new NoopProvider();

  public static IAuthProvider provider(final ISettings settings, final IReflection reflection) {
    final String serviceAuthProvider = settings.getSettingAs("service.auth.provider", String.class);
    if (serviceAuthProvider == null || serviceAuthProvider.trim().isEmpty()) {
      return NOOP_PROVIDER;
    }

    // check if we have a plugin of this name that we can use
    final IPlugin plugin = settings.getPlugin(serviceAuthProvider);
    if (plugin != null) {
      if (plugin instanceof IAuthProvider) {
        return (IAuthProvider) plugin;
      } else {
        throw new DeepRuntimeException(
            String.format("Cannot use plugin %s as auth provider, must implement IAuthProvider interface.", plugin.name()));
      }
    }

    try {
      final Class<?> aClass = Class.forName(serviceAuthProvider);
      final Constructor<?> constructor = ReflectionUtils.findConstructor(aClass, reflection);
      return ReflectionUtils.callConstructor(constructor, settings, reflection);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(String.format("Cannot load auth provider %s", serviceAuthProvider), e);
    }
  }

  static class NoopProvider implements IAuthProvider {

    @Override
    public Map<String, String> provide() {
      return Collections.emptyMap();
    }
  }
}
