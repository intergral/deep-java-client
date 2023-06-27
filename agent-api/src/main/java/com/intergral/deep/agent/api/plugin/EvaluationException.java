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

/**
 * This exception is thrown when a plugin tried to evaluate an expression that fails.
 */
public class EvaluationException extends Exception {

  /**
   * The expression that was evaluated.
   */
  private final String expression;

  /**
   * Create a new exception
   *
   * @param expression the expression that failed
   * @param cause      the failure
   */
  public EvaluationException(final String expression, final Throwable cause) {
    super("Could not evaluate expression: " + expression, cause);
    this.expression = expression;
  }

  /**
   * Get the expression
   *
   * @return {@link #expression}
   */
  public String getExpression() {
    return expression;
  }
}
