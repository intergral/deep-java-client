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

package com.intergral.deep.agent.api.plugin;

import java.util.Map;

/**
 * This defines an evaluator, and evaluator is used to evaluate expression at runtime. ie allows a watch or condition to execute within the
 * scope of the tracepoint.
 */
public interface IEvaluator {

  /**
   * Evaluate an expression as a boolean response.
   *
   * @param expression the expression to evaluate
   * @param values     the variables that the expression can evaluate against
   * @return {@code true} if the expression evaluates to truthy value.
   * @see AbstractEvaluator#objectToBoolean(Object)
   */
  boolean evaluate(final String expression, final Map<String, Object> values);

  /**
   * Evaluate an expression to the value.
   *
   * @param expression the expression to evaluate
   * @param values     the variables that the expression can evaluate against
   * @return the result of the expression
   * @throws Throwable if the expression fails
   */
  Object evaluateExpression(final String expression, final Map<String, Object> values)
      throws Throwable;
}
