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

/**
 * This is how Deep is to be loaded, default provider is in the 'deep' module.
 */
public interface IDeepLoader {

  /**
   * Load the Deep agent into the provided process id.
   *
   * @param pid     the current process id
   * @param config  the config to use
   * @param jarPath the full path to the jar to load (or {@code null} to auto discover the jar)
   * @throws Throwable if loader fails
   */
  void load(final String pid, final String config, final String jarPath) throws Throwable;
}
