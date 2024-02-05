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

package com.intergral.deep.agent.api.logger;

import com.intergral.deep.agent.api.spi.IDeepPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default tracepoint logger that will log to the default Deep logger.
 */
public class TracepointLogger implements ITracepointLogger, IDeepPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(TracepointLogger.class);

  @Override
  public void logTracepoint(final String logMsg, final String tracepointId, final String snapshotId) {
    final String format = String.format("%s snapshot=%s tracepoint=%s", logMsg, tracepointId, snapshotId);
    LOGGER.info(format);
  }
}
