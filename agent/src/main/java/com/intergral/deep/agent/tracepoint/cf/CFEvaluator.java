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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOGGER.debug("Unable to evaluate expression {}", expression);
    }
    return null;
  }
}
