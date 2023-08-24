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

import com.intergral.deep.agent.tracepoint.cf.CFUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFClassScanner implements IClassScanner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CFClassScanner.class);
  protected final Map<String, TracePointConfig> tracePointConfigMap;


  public CFClassScanner(final Map<String, TracePointConfig> tracePointConfigMap) {
    this.tracePointConfigMap = tracePointConfigMap;
  }


  @Override
  public boolean scanClass(final Class<?> loadedClass) {
    // if the map is empty we have already found all our tracepoints
    if (tracePointConfigMap.isEmpty()) {
      // stop as soon as we run out of classes
      return false;
    }

    // if this is a CF class then process it
    if (CFUtils.isCfClass(loadedClass.getName())) {
      try {
        // try to load CF tracepoint - we try-catch as sometimes cfm won't give us a location we can use
        final Set<TracePointConfig> breakpoints = loadCfTracepoint(loadedClass, tracePointConfigMap);
        // if we found some tracepoints
        if (!breakpoints.isEmpty()) {
          // remove them from our config so we can end fast
          for (TracePointConfig breakpoint : breakpoints) {
            tracePointConfigMap.remove(breakpoint.getId());
          }
          return true;
        }
      } catch (Exception e) {
        LOGGER.error("Error processing class {}", loadedClass, e);
      }
    }

    return false;
  }


  /**
   * We need to convert the class name from CF which is normally some hashed version of the file path e.g. cftestList2ecfm1060358347, into a
   * file path to source.
   *
   * @param loadedClass the class we are processing
   * @param values      the current tracepoint configs we are looking to match
   * @return the matched tracepoints
   */
  private Set<TracePointConfig> loadCfTracepoint(
      final Class<?> loadedClass,
      final Map<String, TracePointConfig> values
  ) {
    final URL location = getLocation(loadedClass);
    // Lucee will not give us the code location using protection domain
    if (location == null) {
      // if we cannot get the code source then we guess the source using the provided path name
      return CFUtils.loadCfBreakpoints(
          CFUtils.guessSource(loadedClass.getName()),
          values);
    }
    // Adobe CF should provide the location to the source, so we can use that.
    // todo it is possible to run precompiled CF code which would possibly have a different source location
    return CFUtils.loadCfBreakpoints(location, values);
  }


  URL getLocation(final Class<?> loadedClass) {
    return getLocation(loadedClass.getProtectionDomain());
  }


  URL getLocation(final ProtectionDomain protectionDomain) {
    return protectionDomain.getCodeSource().getLocation();
  }

  @Override
  public boolean isComplete() {
    return tracePointConfigMap.isEmpty();
  }
}
