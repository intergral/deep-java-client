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

package com.intergral.deep.agent.tracepoint.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.handler.FrameCollector.IExpressionResult;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.test.MockTracepointConfig;
import com.intergral.deep.test.target.ConditionTarget;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FrameCollectorTest {

  private Settings settings = Mockito.mock(Settings.class);
  private IEvaluator evaluator = Mockito.mock(IEvaluator.class);
  private FrameCollector frameCollector;

  @BeforeEach
  void setUp() {
    Mockito.when(settings.getResource()).thenReturn(Resource.DEFAULT);
    frameCollector = new FrameCollector(settings, evaluator, Collections.singletonMap("this", new ConditionTarget()),
        Thread.currentThread().getStackTrace());
    frameCollector.configureSelf(Collections.singletonList(new MockTracepointConfig()));
  }

  @Test
  void evaluateWatchers() throws Throwable {
    Mockito.when(evaluator.evaluateExpression(Mockito.anyString(), Mockito.anyMap())).thenReturn("some result");
    final IExpressionResult someExpression = frameCollector.evaluateWatchExpression("some expression");
    assertEquals("some expression", someExpression.result().expression());
    assertEquals(1, someExpression.variables().size());
    final Variable variable = someExpression.variables().get("1");
    assertEquals("some result", variable.getValString());
  }

  @Test
  void evaluateWatchers_error() throws Throwable {
    Mockito.when(evaluator.evaluateExpression(Mockito.anyString(), Mockito.anyMap())).thenThrow(new RuntimeException("Test exception"));
    final IExpressionResult someExpression = frameCollector.evaluateWatchExpression("some expression");
    assertEquals("some expression", someExpression.result().expression());
    assertEquals(0, someExpression.variables().size());
    assertEquals("java.lang.RuntimeException: Test exception", someExpression.result().error());
  }
}