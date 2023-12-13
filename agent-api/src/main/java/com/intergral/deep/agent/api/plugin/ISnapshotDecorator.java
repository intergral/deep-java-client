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

package com.intergral.deep.agent.api.plugin;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;

/**
 * This type allows a plugin to provide additional attributes to captured snapshots.
 */
public interface ISnapshotDecorator {

  /**
   * This method is called by Deep after a snapshot is created.
   * <p>
   * This method is executed inline with the tracepoint code.
   *
   * @param settings the current settings of Deep
   * @param snapshot the {@link ISnapshotContext} describing the snapshot
   * @return a new {@link Resource} to be added to the snapshot, or {@code null} to do nothing
   */
  Resource decorate(final ISettings settings, final ISnapshotContext snapshot);
}
