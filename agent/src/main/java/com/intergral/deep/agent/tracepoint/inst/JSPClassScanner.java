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


import com.intergral.deep.agent.tracepoint.inst.jsp.JSPUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This scanner is meant to find JSP classes that have tracepoints.
 */
public class JSPClassScanner implements IClassScanner {

  private final Map<String, TracePointConfig> tracePointConfigMap;
  private final String jspSuffix;
  private final List<String> jspPackages;


  public JSPClassScanner(final Map<String, TracePointConfig> tracepoints,
      final String jspSuffix,
      final List<String> jspPackages) {
    this.tracePointConfigMap = tracepoints;
    this.jspSuffix = jspSuffix;
    this.jspPackages = jspPackages;
  }


  @Override
  public boolean scanClass(final Class<?> loadedClass) {
    // if the map is empty we have already found all our tracepoints
    if (tracePointConfigMap.isEmpty()) {
      // stop as soon as we run out of classes
      return false;
    }
    // if this class is a jsp class then we should process it
    if (JSPUtils.isJspClass(this.jspSuffix, this.jspPackages, loadedClass.getName())) {
      // load (from our config) the JSP version of a tracepoint.
      final Set<TracePointConfig> tracePointConfigs = JSPUtils.loadJSPTracepoints(loadedClass, tracePointConfigMap);
      // if we have some tracepoints
      if (!tracePointConfigs.isEmpty()) {
        // remove them from our config so we can end fast
        for (TracePointConfig tracePointConfig : tracePointConfigs) {
          tracePointConfigMap.remove(tracePointConfig.getId());
        }
        // this class is a JSP class with at least on tracepoints
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isComplete() {
    return tracePointConfigMap.isEmpty();
  }
}
