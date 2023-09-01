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

package com.intergral.deep.agent.types.snapshot;

/**
 * The result of a watch expression evaluation.
 */
public class WatchResult {

  private final String error;
  private final VariableID result;
  private final String expression;


  /**
   * Create a bad result.
   *
   * @param expression the expression
   * @param error      the error
   */
  public WatchResult(final String expression, final String error) {
    this.expression = expression;
    this.error = error;
    this.result = null;
  }

  /**
   * Create a good result.
   *
   * @param expression the expression
   * @param result     the result
   */
  public WatchResult(final String expression, final VariableID result) {
    this.expression = expression;
    this.error = null;
    this.result = result;
  }

  public boolean isError() {
    return this.error != null;
  }

  public String error() {
    return this.error;
  }

  public VariableID goodResult() {
    return result;
  }

  public String expression() {
    return this.expression;
  }
}
