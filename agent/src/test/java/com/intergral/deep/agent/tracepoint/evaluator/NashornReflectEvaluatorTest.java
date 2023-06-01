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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intergral.deep.Person;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngineFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NashornReflectEvaluatorTest {

  public final Person person = new Person("bob");
  private final String name = "qqt";

  @Test
  void evalExpression() throws Throwable {
    final IEvaluator iEvaluator = NashornReflectEvaluator.loadEvaluator(
        NashornReflectEvaluatorTest.class.getClassLoader());

    final Map<String, Object> vars = new HashMap<>();
    vars.put("this", NashornReflectEvaluatorTest.this);

    final Object o = iEvaluator.evaluateExpression("this.person.name", vars);

    assertEquals("bob", String.valueOf(o));
  }

  /**
   * This test is more of a note for using nashorn and allows for testing features etc.
   */
  @Test
  @Disabled
  void nashornRandomUsage() throws Exception {
    final HashMap<String, String> hashMap = new HashMap<String, String>() {{
      put("name", "ben");
    }};
    javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
    final List<ScriptEngineFactory> engineFactories = mgr.getEngineFactories();

    for (javax.script.ScriptEngineFactory engineFactory : engineFactories) {
      System.out.println(engineFactory.getNames());
    }

    javax.script.ScriptEngine engine = mgr.getEngineByName("JavaScript");
    final javax.script.Bindings bindings = engine.createBindings();
//        bindings.put( "obj", hashMap );
//        bindings.put( "person", new Person( new Person( "mary" ), "bob" ) );
    bindings.putAll(new HashMap<String, Object>() {{
      put("obj", hashMap);
      put("person", new Person(new Person("mary"), "qwe"));
      put("qq", NashornReflectEvaluatorTest.this);
    }});

    System.out.println(engine.eval("person", bindings));
    System.out.println(engine.eval("person.name", bindings));
    System.out.println(engine.eval("person.getParent()", bindings));
//        System.out.println( engine.eval( "System.exit()", bindings ) );
    final Object eval = engine.eval("qq", bindings);
    System.out.println(eval);
    System.out.println(engine.eval("qq.name", bindings));
    System.out.println(engine.eval("person.parent.name == 'mary'", bindings));
  }
}