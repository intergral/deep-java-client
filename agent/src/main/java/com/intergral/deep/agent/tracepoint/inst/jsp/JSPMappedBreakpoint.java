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

package com.intergral.deep.agent.tracepoint.inst.jsp;

import com.intergral.deep.agent.types.TracePointConfig;

/**
 * This is a simple wrapper of the tracepoint config with the mapped line we need to use.
 */
public class JSPMappedBreakpoint extends TracePointConfig {

  private final int mappedLine;


  public JSPMappedBreakpoint(final TracePointConfig tp, final int mappedLine) {
    super(tp.getId(), tp.getPath(), tp.getLineNo(), tp.getArgs(), tp.getWatches());
    this.mappedLine = mappedLine;
  }

  @Override
  public int getLineNo() {
    return this.mappedLine;
  }
}
