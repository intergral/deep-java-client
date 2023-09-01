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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractEvaluatorTest {


  private AbstractEvaluator abstractEvaluator;

  @BeforeEach
  void setUp() {
    abstractEvaluator = new AbstractEvaluator() {
      @Override
      public Object evaluateExpression(final String expression, final Map<String, Object> values) {
        return null;
      }
    };
  }

  @Test
  void evaluate() throws Throwable {
    assertNull(abstractEvaluator.evaluateExpression("anything", new HashMap<>()));
    assertFalse(abstractEvaluator.evaluate("anything", new HashMap<>()));
  }

  @Test
  void objectToBoolean() {
    assertFalse(LazyEvaluator.objectToBoolean(null));
    assertFalse(LazyEvaluator.objectToBoolean(0));
    assertFalse(LazyEvaluator.objectToBoolean(false));
    assertFalse(LazyEvaluator.objectToBoolean("false"));

    assertTrue(LazyEvaluator.objectToBoolean(1));
    assertTrue(LazyEvaluator.objectToBoolean(true));
    assertTrue(LazyEvaluator.objectToBoolean("true"));
  }
}