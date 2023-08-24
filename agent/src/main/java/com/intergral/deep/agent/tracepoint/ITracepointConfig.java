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

package com.intergral.deep.agent.tracepoint;

import com.intergral.deep.agent.types.TracePointConfig;
import java.util.Collection;

/**
 * This is the interface to the config services. The implementation of this service should act as the manager between incoming tracepoint
 * configs and instrumentation. To help reduce the time spent in the instrumentor.
 */
public interface ITracepointConfig {

  /**
   * Called when there is no change to the config so just update last seen.
   *
   * @param tsNano the time of the config update
   */
  void noChange(final long tsNano);

  /**
   * This indicates that the config from the servers has changed, and we should inform the instrumentation services.
   *
   * @param tsNano      the time of the update
   * @param hash        the new config hash
   * @param tracepoints the new config
   */
  void configUpdate(final long tsNano, final String hash, final Collection<TracePointConfig> tracepoints);

  /**
   * Get the hash of the config last used to update the config. This hash should be sent with the calls for new configs, so the server knows
   * what the clients config is and can detect changes.
   *
   * @return the current hash.
   */
  String currentHash();

  /**
   * Load the full configs for the given tracepoints ids
   *
   * @param tracepointId the tracepoint ids
   * @return a collection of all the matched tracepoints
   */
  Collection<TracePointConfig> loadTracepointConfigs(final Collection<String> tracepointId);
}
