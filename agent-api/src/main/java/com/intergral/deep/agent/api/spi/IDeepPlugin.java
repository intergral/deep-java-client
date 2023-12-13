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

package com.intergral.deep.agent.api.spi;

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.settings.ISettings;

/**
 * This type defines a plugin for Deep. These plugins will be loaded via SPI and can provide any functionality from the extension points in
 * Deep.
 *
 * <ul>
 *   <li>{@link IConditional} - allow plugin to be conditional</li>
 *   <li>{@link com.intergral.deep.agent.api.plugin.ISnapshotDecorator} - allow plugins to provide additional attributes to captured snapshots</li>
 *   <li>{@link com.intergral.deep.agent.api.auth.IAuthProvider} - allow plugin to provide additional ways to authenticate</li>
 *   <li>{@link ResourceProvider} - allow plugins to provide additional information for the resource definition</li>
 * </ul>
 * <p>
 * Plugins will be instantiated via the default constructor and then the {@link #configure(ISettings, IReflection)} function will be invoked.
 */
public interface IDeepPlugin extends Ordered {

  /**
   * This allows for the plugin to retain a reference to the settings for Deep and allows access to the {@link IReflection} service to
   * perform reflection operations.
   *
   * @param settings   the settings for Deep
   * @param reflection a service to allow easier access to reflection
   * @return {@code this}, or a new instance of a plugin
   *
   * @see IReflection
   * @see ISettings
   */
  default IDeepPlugin configure(ISettings settings, IReflection reflection) {
    return this;
  }
}
