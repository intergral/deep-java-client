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
 * This allows for common handling for object to boolean expressions.
 */
public abstract class AbstractEvaluator implements IEvaluator {

  @Override
  public boolean evaluate(final String expression, final Map<String, Object> values) {
    try {
      return objectToBoolean(evaluateExpression(expression, values));
    } catch (Throwable e) {
      return false;
    }
  }


  /**
   * Given an input convert to a boolean expression.
   * <p>
   * As Java doesn't have inherit truthiness such as JavaScript. We want to simulate it here, we will convert the following to inputs, all
   * other inputs are {@code true}.
   * <ul>
   *   <li>null - All null values are {@code false}.</li>
   *   <li>boolean = All booleans are returned as they are.</li>
   *   <li>0 = All numbers are {@code true}, except 0 which is {@code false}</li>
   *   <li>"true" = A string of the value {@code "true"} (ignoring case) is {@code true}, all other strings are {@code false}.</li>
   * </ul>
   *
   * @param obj the value to convert
   * @return the value as  a boolean
   * @see Boolean#parseBoolean(String)
   */
  public static boolean objectToBoolean(final Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }

    if (obj instanceof Number) {
      return ((Number) obj).intValue() != 0;
    }

    if (obj instanceof String) {
      return Boolean.parseBoolean(String.valueOf(obj));
    }

    return true;
  }
}
