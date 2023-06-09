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


import static com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService.loadJspBreakpoints;

import com.intergral.deep.agent.tracepoint.inst.jsp.JSPUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JSPClassScanner implements IClassScanner {

  private final Map<String, TracePointConfig> removedBreakpoints;
  private final String jspSuffix;
  private final List<String> jspPackages;


  public JSPClassScanner(final Map<String, TracePointConfig> removedBreakpoints,
      final String jspSuffix,
      final List<String> jspPackages) {
    this.removedBreakpoints = removedBreakpoints;
    this.jspSuffix = jspSuffix;
    this.jspPackages = jspPackages;
  }


  @Override
  public boolean scanClass(final Class<?> loadedClass) {
    if (removedBreakpoints.isEmpty()) {
      // stop as soon as we run out of classes
      return false;
    }
    if (JSPUtils.isJspClass(this.jspSuffix, this.jspPackages, loadedClass.getName())) {
      final Set<TracePointConfig> breakpoints = loadJspBreakpoints(loadedClass, removedBreakpoints);
      if (!breakpoints.isEmpty()) {
        for (TracePointConfig breakpoint : breakpoints) {
          removedBreakpoints.remove(breakpoint.getId());
        }
        return true;
      }
    }
    return false;
  }
}
