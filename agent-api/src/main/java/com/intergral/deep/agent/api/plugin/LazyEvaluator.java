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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type allows the evaluator to be loaded only if it is needed. ie. if we have no watches or conditions there is no need for an
 * evaluator.
 */
public class LazyEvaluator extends AbstractEvaluator {

  private final static Logger LOGGER = LoggerFactory.getLogger(LazyEvaluator.class);
  private static final Exception NO_EVALUATOR_EXCEPTION = new RuntimeException(
      "No evaluator available.");
  private final IEvaluatorLoader loader;
  private IEvaluator evaluator;

  public LazyEvaluator(final IEvaluatorLoader loader) {
    this.loader = loader;
  }

  private IEvaluator load() {
    if (this.evaluator == null) {
      try {
        this.evaluator = this.loader.load();
      } catch (Exception e) {
        LOGGER.error("Could not load evaluator", e);
      }
      if (this.evaluator == null) {
        this.evaluator = new AbstractEvaluator() {
          @Override
          public Object evaluateExpression(final String expression, final Map<String, Object> values) throws Throwable {
            throw NO_EVALUATOR_EXCEPTION;
          }
        };
    }
    }
    return this.evaluator;
  }

  @Override
  public Object evaluateExpression(final String expression, final Map<String, Object> values) throws Throwable {
    return load().evaluateExpression(expression, values);
  }

  public interface IEvaluatorLoader {

    IEvaluator load();
  }
}
