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

package com.intergral.deep.agent;

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.reflect.ReflectionImpl;

/**
 * A collection of utils that simplify the use of reflection.
 */
public final class Reflection {

  private Reflection() {
  }

  private static final IReflection reflection;

  static {
    // we need to change the reflection service we use based on version of java
    if (Utils.getJavaVersion() >= 9) {
      // if we are version 9 or more. Then use the version 9 reflection
      reflection = new com.intergral.deep.reflect.Java9ReflectionImpl();
    } else {
      // else use the java 8 version
      reflection = new ReflectionImpl();
    }
  }

  /**
   * Get the active version of reflection to use.
   *
   * @return the reflection service.
   */
  public static IReflection getInstance() {
    return reflection;
  }
}
