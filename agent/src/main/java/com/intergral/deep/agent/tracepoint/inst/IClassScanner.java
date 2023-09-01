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

package com.intergral.deep.agent.tracepoint.inst;

/**
 * Used to define a method to scan the loaded classes.
 */
public interface IClassScanner {

  /**
   * Scan this class.
   *
   * @param clazz the class to sacn
   * @return {@code true} if we should include this class
   */
  boolean scanClass(final Class<?> clazz);

  /**
   * Is this class scanner complete.
   *
   * @return {@code true} if this scanner has nothing more to find
   */
  boolean isComplete();
}
