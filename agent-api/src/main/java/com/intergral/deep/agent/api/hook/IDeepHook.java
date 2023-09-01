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

package com.intergral.deep.agent.api.hook;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.reflection.IReflection;

/**
 * This type is used to pass an object from the agent to the API. It should not be directly used by clients.
 */
public interface IDeepHook {

  /**
   * Get the deep service from the agent.
   *
   * @return the deep agent.
   */
  IDeep deepService();

  /**
   * Get the configured reflection api.
   *
   * @return the reflection api.
   */
  IReflection reflectionService();
}
