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

package com.intergral.deep.agent.api.reflection;

import com.intergral.deep.agent.api.DeepRuntimeException;
import com.intergral.deep.agent.api.settings.ISettings;
import java.lang.reflect.Constructor;

/**
 * A simple util to get create plugins. A plugin can have a default constructor or one that accepts the {@link ISettings} class. So this
 * type helps us find and call the appropriate constructor.
 */
public final class ReflectionUtils {

  private ReflectionUtils() {
  }

  /**
   * Call the constructor.
   *
   * @param constructor the constructor
   * @param settings    the settings object
   * @param reflection  the reflection service to use
   * @param <T>         the type of the new object
   * @return the new object
   */
  public static <T> T callConstructor(final Constructor<?> constructor, final ISettings settings, final IReflection reflection) {
    if (constructor.getParameterTypes().length == 0) {
      return reflection.callConstructor(constructor);
    }
    return reflection.callConstructor(constructor, settings);
  }

  /**
   * Find the constructor to use.
   * <p>
   * Will look for the constructor that we can use in the order:
   * <ul>
   *   <li>constructor(ISettings.class)</li>
   *   <li>constructor()</li>
   * </ul>
   *
   * @param clazz      the class to look on
   * @param reflection the reflection service to use
   * @return the constructor that was found
   * @throws DeepRuntimeException if we could not find a constructor
   */
  public static Constructor<?> findConstructor(final Class<?> clazz, final IReflection reflection) {

    final Constructor<?> constructor = reflection.findConstructor(clazz, ISettings.class);
    if (constructor != null) {
      return constructor;
    }

    final Constructor<?> defaultConstructor = reflection.findConstructor(clazz);
    if (defaultConstructor != null) {
      return defaultConstructor;
    }
    final String simpleName = clazz.getSimpleName();
    throw new DeepRuntimeException(
        String.format("Cannot create auth provider of type: %s. Class is missing constructor %s(%s) or %s().", clazz.getName(),
            simpleName, ISettings.class.getName(), simpleName));

  }

}
