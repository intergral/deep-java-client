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

package com.intergral.deep.agent.tracepoint.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.tracepoint.cf.CFEvaluator.Loader;
import java.util.HashMap;
import java.util.Map;
import lucee.runtime.PageContextImpl;
import lucee.runtime.PageImpl;
import org.junit.jupiter.api.Test;

class CFEvaluatorTest {

  @Test
  void coverage() throws NoSuchMethodException {
    final CFEvaluator evaluate = new CFEvaluator(this, this.getClass().getDeclaredMethod("evaluate", String.class));
    assertEquals("input", evaluate.evaluateExpression("input", new HashMap<>()));
    assertNull(evaluate.evaluateExpression("error", new HashMap<>()));
  }

  @Test
  void loader() {
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", new PageImpl.SomePageImpl());
    hashMap.put("param0", new PageContextImpl());
    final Loader loader = new Loader(hashMap);
    final IEvaluator load = loader.load();
    assertNotNull(load);
  }

  public Object evaluate(String input) {
    if (input.equals("error")) {
      throw new RuntimeException("test exception");
    }
    return input;
  }
}