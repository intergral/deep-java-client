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
 * This is the context passed to plugins. This allows for the data of a context to be exposed to the plugin in a controlled manor.
 */
public interface ISnapshotContext {

  /**
   * Evaluate an expression in the frame of the tracepoint that triggered this snapshot.
   *
   * @param expression the express to evaluate
   * @return the result of the expression as a string
   * @throws EvaluationException if there were any issues evaluating the expression
   */
  String evaluateExpression(String expression) throws EvaluationException;
}
