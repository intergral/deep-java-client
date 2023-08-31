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

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.types.TracePointConfig;

public final class TracepointUtils {

  private TracepointUtils() {
  }

  /**
   * We normally get set the source file name, we need to convert this to a Java class name.
   *
   * @param tp the tracepoint to process
   * @return the internal class name to install the tracepoint in, or {@code cfm} or {@code jsp} if the computed class is a CFM or JSP
   * class.
   */
  public static String estimatedClassRoot(final TracePointConfig tp) {
    // we allow the class name to be sent for specific cases
    final String className = tp.getArg("class_name", String.class, null);
    if (className != null) {
      return InstUtils.internalClass(className);
    }

    return asFullClassName(tp);
  }


  private static String asFullClassName(TracePointConfig tp) {
    return parseFullClassName(tp.getPath(), tp.getArg("src_root", String.class, null));
  }


  private static String parseFullClassName(final String rawRelPath, final String srcRootArg) {
    // cf classes are handled specially
    if (rawRelPath.endsWith(".cfm") || rawRelPath.endsWith(".cfc")) {
      return "cfm";
    }
    // jsp classes are handled specially
    if (rawRelPath.endsWith(".jsp")) {
      return "jsp";
    }

    final int endIndex = rawRelPath.lastIndexOf('.');
    final String relPath;
    // users can install tracepoints in files that do not have extensions
    // this just handles it so we have a file/class name for them
    // even though they wont be installed by the agent
    if (endIndex == -1) {
      relPath = rawRelPath;
    } else {
      relPath = rawRelPath.substring(0, endIndex);
    }
    if (srcRootArg != null) {
      if (relPath.startsWith(srcRootArg)) {
        return Utils.trimPrefix(relPath.substring(srcRootArg.length()), "/");
      }
    } else if (relPath.contains("/src/main/")) {
      final String mainDir = relPath.substring(relPath.indexOf("/src/main/") + 11);
      final int i = mainDir.indexOf('/');
      return Utils.trimPrefix(mainDir.substring(i), "/");
    } else if (relPath.contains("/src/test/")) {
      final String mainDir = relPath.substring(relPath.indexOf("/src/test/") + 11);
      final int i = mainDir.indexOf('/');
      return Utils.trimPrefix(mainDir.substring(i), "/");
    }
    final String trim = Utils.trimPrefix(relPath, "/");
    // this is just to ensure the file name is never empty
    // this only happens on non class files such as '.gitignore'
    // rather then return empty name we return the raw path
    if (trim.isEmpty()) {
      return Utils.trimPrefix(rawRelPath, "/");
    }
    return trim;
  }
}
