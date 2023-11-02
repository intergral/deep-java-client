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
 * Tracepoint logger is used to log the result of an injected log message.
 */
public interface ITracepointLogger {

  /**
   * Log the result of a tracepoint injected log message.
   *
   * @param logMsg       the processed log message
   * @param tracepointId the tracepoint id that triggered the log
   * @param snapshotId   the snapshot id of the generated snapshot from the log
   */
  void logTracepoint(final String logMsg, final String tracepointId, final String snapshotId);
}
