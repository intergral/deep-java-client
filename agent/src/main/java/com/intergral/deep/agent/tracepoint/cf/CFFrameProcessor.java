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
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.handler.FrameProcessor;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * We want to map the variables from the Java variables to the CF variables. So we have a custom processor for CF that lets us perform this
 * mapping.
 */
public class CFFrameProcessor extends FrameProcessor {

  public CFFrameProcessor(final Settings settings,
      final IEvaluator evaluator,
      final Map<String, Object> variables,
      final Collection<TracePointConfig> tracePointConfigs,
      final long[] lineStart, final StackTraceElement[] stack) {
    super(settings, evaluator, variables, tracePointConfigs, lineStart, stack);
  }

  @Override
  protected String getMethodName(final StackTraceElement stackTraceElement,
      final Map<String, Object> variables,
      final int frameIndex) {
    final String udf = CFUtils.findUdfName(variables, stackTraceElement.getClassName(), frameIndex);
    if (udf != null) {
      return udf;
    } else {
      return stackTraceElement.getMethodName();
    }
  }

  @Override
  protected boolean isAppFrame(final StackTraceElement stackTraceElement) {
    return CFUtils.isCFFile(stackTraceElement.getFileName());
  }

  @Override
  protected Map<String, Object> selectVariables(final int frameIndex) {
    if (frameIndex != 0 || frameConfig.isCfRaw()) {
      return super.selectVariables(frameIndex);
    }

    return mapCFScopes(this.variables);
  }


  /**
   * When processing a CF snapshot we want to add the variables as seen by a CF dev, these are a set
   * of scopes that are accessed via a context.
   *
   * @param variables the variables
   * @return the variables modified to the scope set
   */
  Map<String, Object> mapCFScopes(final Map<String, Object> variables) {
    final Object aThis = variables.get("this");
    if (CFUtils.isLucee(aThis)) {
      // lucee requires special handling
      return convertLuceeScopes(variables);
    }

    // handle local scope
    final Map<String, Object> cfVars = new HashMap<>();
    final Object localScope = variables.get("__localScope");

    if (CFUtils.isScope(localScope)) {
      final Map<Object, Object> lclMap = (Map) localScope;
      for (Map.Entry<Object, Object> entry : lclMap.entrySet()) {
        final Object name = entry.getKey();
        final Object value = entry.getValue();
        cfVars.put(Utils.valueOf(name), value);
      }
    }

    // other scopes are on the context to find that
    final Object pageContext = CFUtils.findPageContext(variables);
    if (pageContext == null) {
      return cfVars;
    }

    // handle var scope
    final Object varScope = ReflectionUtils.getFieldValue(pageContext, "SymTab_varScope");

    if (CFUtils.isScope(varScope)) {
      cfVars.put("VARIABLES", varScope);
    }

    // find the other build in scopes
    final Map<Object, Object> scopes = ReflectionUtils.getFieldValue(pageContext,
        "SymTab_builtinCFScopes");
    if (scopes == null) {
      return cfVars;
    }

    // handle each of the scopes
    for (Map.Entry<Object, Object> entry : scopes.entrySet()) {
      final Object name = entry.getKey();
      final Object value = entry.getValue();

      // cheap hack to check class is a CF scope
      if (!CFUtils.isScope(value)) {
        continue;
      }
      cfVars.put(Utils.valueOf(name), value);
    }

    return cfVars;
  }


  /**
   * Special handling for lucee scopes.
   *
   * @param variables the variables
   * @return the scopes for lucee
   */
  Map<String, Object> convertLuceeScopes(final Map<String, Object> variables) {
    // in lucee variable names are removed, but we know that 'param0' is the page context
    final Object param0 = variables.get("param0");
    if (param0 == null) {
      return new HashMap<>();
    }

    final Map<String, Object> scopes = new HashMap<>();
    // process the scopes from lucee
    scopes.put("variables", ReflectionUtils.getFieldValue(param0, "variables"));
    scopes.put("argument", ReflectionUtils.getFieldValue(param0, "argument"));
    scopes.put("local", getAndCheckLocal("local", param0));
    scopes.put("cookie", getAndCheckScope("cookie", param0));
    scopes.put("server", ReflectionUtils.getFieldValue(param0, "server"));
    scopes.put("session", getAndCheckScope("session", param0));
    scopes.put("application", ReflectionUtils.getFieldValue(param0, "application"));
    scopes.put("cgi", getAndCheckScope("cgiR", param0));
    scopes.put("request", getAndCheckScope("request", param0));
    scopes.put("form", getAndCheckScope("_form", param0));
    scopes.put("url", getAndCheckScope("_url", param0));
    scopes.put("client", getAndCheckScope("client", param0));
    scopes.put("threads", getAndCheckScope("threads", param0));

    return scopes;
  }


  /**
   * This method will get anc check that the field is a local scope as some parts can have no local
   * scope.
   *
   * @param local  the name of the field to look for
   * @param param0 the object to look at
   * @return {@code null} if the field is not a valid local scope
   */
  private Object getAndCheckLocal(final String local, final Object param0) {
    final Object o = ReflectionUtils.getFieldValue(param0, local);
    if (o == null || o.getClass().getName()
        .equals("lucee.runtime.type.scope.LocalNotSupportedScope")) {
      return null;
    }
    return o;
  }


  /**
   * This method loads and checks the scope for lucee, as it is possible that the scope is not
   * initialised. If it is not then the scope can be in an invalidate state and should be ignored.
   *
   * @param name   the name of the field to look for
   * @param target the object to look for
   * @return {@code null} if the field is not a scope or is not initialised, else the scope
   *     discovered.
   */
  private Object getAndCheckScope(final String name, final Object target) {
    final Object local = ReflectionUtils.getFieldValue(target, name);
    if (local != null) {
      final Object isInitalized = ReflectionUtils.callMethod(local, "isInitalized");
      if (isInitalized == null) {
        return null;
      }

      final boolean isInitalizedRtn = Boolean.parseBoolean(String.valueOf(isInitalized));
      if (isInitalizedRtn) {
        return local;
      }
    }
    return null;
  }
}
