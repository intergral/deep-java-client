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

import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SmapUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMap;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMapParser;
import com.intergral.deep.agent.types.TracePointConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSPUtils {

  private JSPUtils() {
  }


  public static boolean isJspClass(final String jspSuffix,
      final List<String> jspPackages,
      final String loadedClassName) {
    return getJspClassname(jspSuffix, jspPackages, loadedClassName) != null;
  }


  private static String getJspClassname(final String jspSuffix,
      final List<String> jspPackages,
      final String className) {
    if (jspSuffix.isEmpty() || className.endsWith(jspSuffix)) {
      for (final String jspPackage : jspPackages) {
        if (className.startsWith(jspPackage)) {
          return className.substring(jspPackage.length() + 1);
        }
      }
      if (className.startsWith("/")) {
        return className.substring(1);
      }
    }
    return null;
  }


  public static SourceMap getSourceMap(final Class<?> c) {
    try {
      final String rtn = SmapUtils.lookUp(c);
      final SourceMapParser parser;
      if (rtn != null) {
        parser = new SourceMapParser(rtn);
        return parser.parse();
      }
      return null;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }


  public static SourceMap getSourceMap(byte[] classfileBuffer) {
    try {
      final String rtn = SmapUtils.parseBytes(classfileBuffer);
      final SourceMapParser parser = new SourceMapParser(rtn);
      return parser.parse();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  public static Set<TracePointConfig> loadJSPTracepoints(final Class<?> loadedClass,
      final Map<String, TracePointConfig> jsp) {
    return loadJSPTracepoints(getSourceMap(loadedClass), jsp);
  }

  public static Set<TracePointConfig> loadJSPTracepoints(final SourceMap sourceMap,
      final Map<String, TracePointConfig> jsp) {
    if (sourceMap == null) {
      return Collections.emptySet();
    }

    final Set<TracePointConfig> matchedJsp = new HashSet<>();
    final List<String> filenames = sourceMap.getFilenames();
    for (Map.Entry<String, TracePointConfig> entry : jsp.entrySet()) {
      final TracePointConfig value = entry.getValue();
      final String fileName = InstUtils.fileName(value.getPath());
      if (filenames.contains(fileName)) {
        matchedJsp.add(value);
      }
    }
    return matchedJsp;
  }
}
