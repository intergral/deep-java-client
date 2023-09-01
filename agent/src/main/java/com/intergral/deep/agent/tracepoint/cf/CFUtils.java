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

package com.intergral.deep.agent.tracepoint.cf;

import com.intergral.deep.agent.ReflectionUtils;
import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.types.TracePointConfig;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities to help with CF related item.
 */
public final class CFUtils {

  private CFUtils() {
  }


  /**
   * Find the evaluator to use for CF.
   * <p>
   * Cf provides an {@code Evaluate} method on the page object that can be used to evaluate strings. This method tries to find that method.
   *
   * @param variables the variables to scan
   * @return the evaluate to use, or {@code null}
   */
  public static IEvaluator findCfEval(final Map<String, Object> variables) {
    final Object that = variables.get("this");
    if (isLucee(that)) {
      return findLuceeEvaluator(variables);
    }
    final Object page = CFUtils.findPage(variables);
    if (page == null) {
      return null;
    }

    Method evaluate = ReflectionUtils.findMethod(page.getClass(), "Evaluate", String.class);
    if (evaluate == null) {
      evaluate = ReflectionUtils.findMethod(page.getClass(), "Evaluate", Object.class);
      if (evaluate == null) {
        return null;
      }
    }
    return new CFEvaluator(page, evaluate);
  }


  private static IEvaluator findLuceeEvaluator(final Map<String, Object> map) {
    final Object param0 = map.get("param0");
    if (param0 == null || !param0.getClass().getName().equals("lucee.runtime.PageContextImpl")) {
      return null;
    }

    final Method evaluate = ReflectionUtils.findMethod(param0.getClass(), "evaluate", String.class);
    if (evaluate == null) {
      return null;
    }

    return new CFEvaluator(param0, evaluate);
  }


  /**
   * CF doesn't use the java method name, so we look for the UDF method name in the variables.
   *
   * @param variables  the variables to look in
   * @param className  the name of the class
   * @param stackIndex the stack index
   * @return the name of the UDF method, or {@code null}
   */
  public static String findUdfName(final Map<String, Object> variables, final String className,
      final int stackIndex) {
    if (stackIndex == 0) {
      final Object aThis = variables.get("this");
      if (aThis == null || !isUdfMethod(aThis)) {
        return null;
      }
      // explicitly set to Object as otherwise the call to String.valueOf can become the char[] version.
      return String.valueOf(ReflectionUtils.<Object>getFieldValue(aThis, "key"));
    } else if (className.startsWith("cf") && className.contains("$func")) {
      return className.substring(className.indexOf("$func") + 5);
    } else {
      return null;
    }
  }


  private static boolean isUdfMethod(final Object object) {
    final Class<?> superclass = object.getClass().getSuperclass();
    return superclass != null && superclass.getName().equals("coldfusion.runtime.UDFMethod");
  }


  static boolean isScope(final Object varScope) {
    return varScope instanceof Map
        && (varScope.getClass().getName().startsWith("coldfusion")
        && varScope.getClass().getName().contains("Scope")
        || varScope.getClass().getName().equals("coldfusion.runtime.ArgumentCollection"));
  }


  /**
   * Find the page object.
   *
   * @param localVars the variables to scan
   * @return the page object or {@code null}
   */
  public static Object findPage(final Map<String, Object> localVars) {
    final Object aThis = localVars.get("this");
    if (aThis == null) {
      return null;
    }
    if (isUdfMethod(aThis)) {
      return localVars.get("parentPage");
    } else {
      return aThis;
    }
  }


  /**
   * Find the page context for cf.
   *
   * @param localVars the variables to search
   * @return the page context or {@code null}
   */
  public static Object findPageContext(final Map<String, Object> localVars) {
    final Object page = findPage(localVars);
    if (page == null) {
      return null;
    }
    return ReflectionUtils.getFieldValue(page, "pageContext");
  }


  /**
   * Is this class a possible cf class.
   *
   * @param classname the class name
   * @return {@code true} if the class is cf
   */
  public static boolean isCfClass(final String classname) {
    return classname.startsWith("cf") || classname.endsWith("$cf");
  }


  /**
   * Is the file a possible CF file.
   *
   * @param fileName the file name to check
   * @return {@code true} if the file is a cf file, else {@code false}
   */
  public static boolean isCFFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    return fileName.endsWith(".cf") || fileName.endsWith(".cfc") || fileName.endsWith(".cfm")
        || fileName.endsWith(".cfml");
  }

  /**
   * Are we a lucee page.
   *
   * @param that the object to check
   * @return {@code true} if we are a lucee object
   */
  public static boolean isLucee(final Object that) {
    return that != null
        && that.getClass().getSuperclass() != null
        && (that.getClass().getSuperclass().getName().equals("lucee.runtime.PageImpl")
        || that.getClass().getSuperclass().getName().equals("lucee.runtime.ComponentPageImpl"));
  }


  /**
   * When running on Lucee servers we can guess the source from the class name.
   *
   * @param classname the class we are processing
   * @return the source file name, or {@code null}
   */
  public static String guessSource(final String classname) {
    // if the classname isn't a lucee class then skip it
    if (!classname.endsWith("$cf")) {
      return null;
    }
    return classname
        .replaceAll("\\.", "/")
        .substring(0, classname.length() - 3)
        .replace("_", ".");
  }


  /**
   * Load the CF tracepoints based on the location url.
   *
   * @param location the location to look for
   * @param values   the tracepoints to look at
   * @return the set of tracepoints that match this location
   */
  public static Set<TracePointConfig> loadCfTracepoints(final URL location,
      final Map<String, TracePointConfig> values) {
    final Set<TracePointConfig> iBreakpoints = new HashSet<>();
    final Collection<TracePointConfig> breakpoints = values.values();
    for (TracePointConfig breakpoint : breakpoints) {
      final String srcRoot = breakpoint.getArgs().get("src_root");
      final String relPathFromNv = breakpoint.getPath();
      final String locationString = location.toString();
      if (srcRoot != null && locationString.endsWith(relPathFromNv.substring(srcRoot.length()))
          || locationString.endsWith(relPathFromNv)
          || relPathFromNv.startsWith("/src/main/cfml")
          && locationString.endsWith(relPathFromNv.substring("/src/main/cfml".length()))
      ) {
        iBreakpoints.add(breakpoint);
      }
    }
    return iBreakpoints;
  }

  /**
   * Load the CF tracepoints based on the location string.
   *
   * @param location the location to look for
   * @param values   the tracepoints to look at
   * @return the set of tracepoints that match this location
   */
  public static Set<TracePointConfig> loadCfTracepoints(
      final String location,
      final Map<String, TracePointConfig> values) {
    if (location == null) {
      return Collections.emptySet();
    }
    final Set<TracePointConfig> iBreakpoints = new HashSet<>();
    final Collection<TracePointConfig> breakpoints = values.values();
    for (TracePointConfig breakpoint : breakpoints) {
      final String relPathFromNv = breakpoint.getPath();
      // some versions of lucee use lowercase file names
      if (Utils.endsWithIgnoreCase(relPathFromNv, location)) {
        iBreakpoints.add(breakpoint);
      }
    }
    return iBreakpoints;
  }
}
