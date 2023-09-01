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

import com.intergral.deep.agent.api.plugin.AbstractEvaluator;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.plugin.LazyEvaluator.IEvaluatorLoader;
import java.lang.reflect.Method;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The evaluator to use when running a CF Callback.
 */
public class CFEvaluator extends AbstractEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CFEvaluator.class);

  private final Object page;
  private final Method evaluate;


  public CFEvaluator(final Object page, final Method evaluate) {
    this.page = page;
    this.evaluate = evaluate;
  }


  @Override
  public Object evaluateExpression(final String expression, final Map<String, Object> values) {
    try {
      return evaluate.invoke(page, expression);
    } catch (Throwable e) {
      LOGGER.debug("Unable to evaluate expression {}", expression);
    }
    return null;
  }

  /**
   * The loader to use when executing a CF Callback.
   */
  public static class Loader implements IEvaluatorLoader {

    private final Map<String, Object> variables;

    public Loader(final Map<String, Object> variables) {

      this.variables = variables;
    }

    @Override
    public IEvaluator load() {
      return CFUtils.findCfEval(variables);
    }
  }
}
