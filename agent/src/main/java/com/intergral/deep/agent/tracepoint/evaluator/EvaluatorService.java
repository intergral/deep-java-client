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

import com.intergral.deep.agent.api.plugin.IEvaluator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvaluatorService {

  private EvaluatorService() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluatorService.class);
  private static final Exception NO_EVALUATOR_EXCEPTION = new RuntimeException(
      "No evaluator available.");

  public static IEvaluator createEvaluator() {
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    final IEvaluator iEvaluator = NashornReflectEvaluator.loadEvaluator(contextClassLoader);
    if (iEvaluator != null) {
      return iEvaluator;
    }

    LOGGER.warn("Cannot load evaluator, Java conditions and watches will not be executed.");
    return new IEvaluator() {

      @Override
      public boolean evaluate(final String expression, final Map<String, Object> values) {
        // return true for all expressions, essentially disabling conditions.
        return true;
      }

      @Override
      public Object evaluateExpression(final String expression, final Map<String, Object> values)
          throws Throwable {
        // throw this exception to show the user that we cannot load the evaluator.
        throw NO_EVALUATOR_EXCEPTION;
      }
    };
  }
}
