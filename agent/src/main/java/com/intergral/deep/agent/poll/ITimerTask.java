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

package com.intergral.deep.agent.poll;

public interface ITimerTask {

  /**
   * This method is called by the {@link DriftAwareThread} at the end of each interval
   *
   * @param now the current time
   * @throws Exception if the task fails
   */
  void run(final long now) throws Exception;


  /**
   * This method is called after the {@link #run(long)} method to allow performance tracking.
   *
   * @param duration          the duration of the last execution
   * @param nextExecutionTime the next calculated execution time
   * @return the modified execution next time
   * @throws Exception if the callback fails
   */
  long callback(final long duration, final long nextExecutionTime) throws Exception;
}
