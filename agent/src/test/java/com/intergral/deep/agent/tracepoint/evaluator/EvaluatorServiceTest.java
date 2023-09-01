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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.plugin.IEvaluator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class EvaluatorServiceTest {

  @Test
  void coverage() throws Throwable {
    final IEvaluator evaluator;
    try (MockedStatic<NashornReflectEvaluator> nashorn = Mockito.mockStatic(NashornReflectEvaluator.class, "loadEvaluator")) {
      nashorn.when(() -> NashornReflectEvaluator.loadEvaluator(Mockito.any())).thenReturn(null);
      evaluator = EvaluatorService.createEvaluator();
    }

    assertTrue(evaluator.evaluate(null, null));
    assertThrows(RuntimeException.class, () -> evaluator.evaluateExpression(null, null));
  }
}