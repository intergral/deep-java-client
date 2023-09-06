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

package com.intergral.deep.api;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.reflection.IReflection;

/**
 * This type provides helper methods to get the api and other exposed APIs from deep. This type MUST not be used until after the agent is
 * loaded or there will be Exceptions thrown.
 */
public class DeepAPI {

  /**
   * Get the reflection API.
   *
   * @return a {@link IReflection} instance for the java version we are running
   */
  public static IReflection reflection() {
    return DeepAPILoader.reflection();
  }

  /**
   * Get the Deep api.
   *
   * @return a {@link IDeep} instance
   */
  public static IDeep api() {
    return DeepAPILoader.api();
  }
}
