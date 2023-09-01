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

package com.intergral.deep.agent.tracepoint.evaluator;

import com.intergral.deep.agent.api.plugin.AbstractEvaluator;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This evaluator uses the Nashorn JS engine that is available in some version of Java.
 * <p>
 * This engine was removed in Java 14.
 * <p>
 * There are limitations of this engine.
 * <p>
 * 1. It can only access public methods/fields 2. It can access 'private' fields only if there is a
 * public getter 3. We have to load this via reflection as we need to load from the app classloader
 * not the boot classloader
 */
public class NashornReflectEvaluator extends AbstractEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(NashornReflectEvaluator.class);
  private static boolean NASHORN_AVAILABLE = true;
  private final Object engine;

  private NashornReflectEvaluator(final Object engine) {
    this.engine = engine;
  }

  /**
   * Load the evaluator.
   *
   * @param loader the class loader to use
   * @return the evaluator or {@code null} if Nashorn is not available.
   */
  public static IEvaluator loadEvaluator(final ClassLoader loader) {
    // this stops us from trying to load nashorn again if it failed
    if (!NASHORN_AVAILABLE) {
      return null;
    }
    try {
      final Class<?> aClass = loader.loadClass("javax.script.ScriptEngineManager");
      final Method getEngineByName = aClass.getMethod("getEngineByName", String.class);
      // can we cache any of these classes/objects ?
      final Object o = aClass.getConstructor().newInstance();
      final Object engine = getEngineByName.invoke(o, "JavaScript");
      return new NashornReflectEvaluator(engine);
    } catch (Exception e) {
      NASHORN_AVAILABLE = false;
      LOGGER.error("Cannot load nashorn engine", e);
      return null;
    }
  }

  private Object createBinding(final Map<String, Object> values) {
    final Map<String, Object> toBind;
    // we have to remap 'this' to something else as 'this' is a JS keyword and will become
    // the 'this' that exists inside the JS environment.
    if (values.containsKey("this")) {
      toBind = new HashMap<>(values);
      final Object removed = toBind.remove("this");
      toBind.put("deep_this", removed);
    } else {
      toBind = values;
    }
    try {
      final Method createBindings = engine.getClass().getMethod("createBindings");
      final Object bindings = createBindings.invoke(engine);
      final Method putAll = bindings.getClass().getMethod("putAll", Map.class);
      putAll.invoke(bindings, toBind);
      return bindings;
    } catch (Exception e) {
      LOGGER.error("Cannot create bindings for nashorn.", e);
      return null;
    }
  }

  @Override
  public Object evaluateExpression(final String expression, final Map<String, Object> values)
      throws Throwable {
    final Object bindings = createBinding(values);
    final Method eval = engine.getClass()
        .getMethod("eval",
            String.class,
            engine.getClass().getClassLoader().loadClass("javax.script.Bindings"));
    return eval.invoke(engine, parseExpression(expression), bindings);
  }

  static String parseExpression(final String expression) {
    // we have to remap 'this' to something else as 'this' is a JS keyword and will become
    // the 'this' that exists inside the JS environment.
    if (!expression.contains("this")) {
      return expression.trim();
    }

    return expression.replaceAll("this", "deep_this").trim();
  }
}
