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

/**
 * Log the tracepoint logs to {@link System#out}.
 */
public class StdOutLogger implements ITracepointLogger {

  @Override
  public void logTracepoint(final String logMsg, final String tracepointId, final String snapshotId) {
    final String format = String.format("%s snapshot=%s tracepoint=%s", logMsg, tracepointId, snapshotId);
    System.out.println(format);
  }
}
