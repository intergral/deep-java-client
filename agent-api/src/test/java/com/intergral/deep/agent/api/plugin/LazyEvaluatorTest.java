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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intergral.deep.agent.api.plugin.LazyEvaluator.IEvaluatorLoader;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LazyEvaluatorTest {

  private IEvaluatorLoader loader;
  private LazyEvaluator lazyEvaluator;

  @BeforeEach
  void setUp() {
    loader = Mockito.mock(IEvaluatorLoader.class);
    Mockito.when(loader.load()).thenReturn(new AbstractEvaluator() {
      @Override
      public Object evaluateExpression(final String expression, final Map<String, Object> values) {
        return null;
      }
    });
    lazyEvaluator = new LazyEvaluator(loader);
  }

  @Test
  void evaluateExpression() throws Throwable {
    assertNull(lazyEvaluator.evaluateExpression("anything", new HashMap<>()));
    assertFalse(lazyEvaluator.evaluate("anything", new HashMap<>()));

    Mockito.verify(loader, Mockito.times(1)).load();
  }

  @Test
  void canHandleFailure() {
    Mockito.when(loader.load()).thenReturn(null);
    final RuntimeException something = assertThrows(RuntimeException.class,
        () -> lazyEvaluator.evaluateExpression("something", new HashMap<>()));
    assertEquals("No evaluator available.", something.getMessage());
  }
}