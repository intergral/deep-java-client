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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coldfusion.runtime.ArgumentCollection;
import coldfusion.runtime.TestScope;
import coldfusion.runtime.UDFMethod;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.test.MockTracepointConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lucee.runtime.PageContextImpl;
import lucee.runtime.PageImpl;
import org.junit.jupiter.api.Test;

class CFUtilsTest {

  @Test
  void isCfClassFile() {
    assertTrue(CFUtils.isCFFile("somefile.cfm"));
    assertTrue(CFUtils.isCFFile("somefile.cfc"));
    assertTrue(CFUtils.isCFFile("somefile.cfml"));

    assertFalse(CFUtils.isCFFile("somefile.cfml.java"));
    assertFalse(CFUtils.isCFFile("somefile_cfml.java"));
    assertFalse(CFUtils.isCFFile("somefile_cfc.java"));
    assertFalse(CFUtils.isCFFile("somefile_cfm.java"));
  }

  @Test
  void isCfFile() {
    assertTrue(CFUtils.isCFFile("something.cfc"));
    assertTrue(CFUtils.isCFFile("something.cfm"));
    assertTrue(CFUtils.isCFFile("something.cf"));
    assertTrue(CFUtils.isCFFile("something.cfml"));
    assertFalse(CFUtils.isCFFile("something.c"));
  }


  @Test
  void findLuceeEval() throws Throwable {
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", new PageImpl.SomePageImpl());
    hashMap.put("param0", new PageContextImpl());
    final IEvaluator cfEval = CFUtils.findCfEval(hashMap);

    assertNotNull(cfEval);

    final Object some_expression = cfEval.evaluateExpression("some Expression", hashMap);
    assertEquals("some Expression", some_expression);

  }


  @Test
  void findCfEval() throws Throwable {
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", new CFEvaluatorTarget());
    final IEvaluator cfEval = CFUtils.findCfEval(hashMap);
    assertNotNull(cfEval);

    final Object test = cfEval.evaluateExpression("test", hashMap);
    assertEquals(CFEvaluatorTarget.class.getName(), test.toString());
  }


  @Test
  void findCfEval2() throws Throwable {
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", new CFEvaluatorTarget2());
    final IEvaluator cfEval = CFUtils.findCfEval(hashMap);
    assertNotNull(cfEval);

    final Object test = cfEval.evaluateExpression("test", hashMap);
    assertEquals(CFEvaluatorTarget2.class.getName(), test.toString());
  }


  @Test
  void findCfEval3() {
    final Map<String, Object> hashMap = new HashMap<>();
    final IEvaluator cfEval = CFUtils.findCfEval(hashMap);
    assertNull(cfEval);
  }


  @Test
  void findCfEval4() {
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", this);
    final IEvaluator cfEval = CFUtils.findCfEval(hashMap);
    assertNull(cfEval);
  }


  @Test
  void findUDFName() {
    assertNull(CFUtils.findUdfName(Collections.emptyMap(), "someClassName", 1));
    assertEquals("FINDFILES", CFUtils.findUdfName(Collections.emptyMap(), "cf123546123$funcFINDFILES", 1));
    final Map<String, Object> hashMap = new HashMap<>();
    hashMap.put("this", this);
    assertNull(CFUtils.findUdfName(hashMap, "cf123546123$funcFINDFILES", 0));

    hashMap.put("this", new CfUdfTest("findUDFName"));
    assertEquals("findUDFName", CFUtils.findUdfName(hashMap, "cf123546123$funcFINDFILES", 0));
  }


  @Test
  void isScope() {
    assertFalse(CFUtils.isScope(this));
    assertFalse(CFUtils.isScope("this"));
    assertFalse(CFUtils.isScope(new HashMap<>()));

    assertTrue(CFUtils.isScope(new TestScope()));
    assertTrue(CFUtils.isScope(new ArgumentCollection()));
  }


  @Test
  void findPage() {
    final Map<String, Object> map = new HashMap<>();
    assertNull(CFUtils.findPage(map));

    map.put("this", this);
    assertSame(this, CFUtils.findPage(map));

    map.put("this", new CfUdfTest("findPage"));

    assertNull(CFUtils.findPage(map));

    map.put("parentPage", this);
    assertSame(this, CFUtils.findPage(map));
  }


  @Test
  void findPageContext() {
    final Map<String, Object> map = new HashMap<>();
    assertNull(CFUtils.findPageContext(map));

    final Object pageContext = new Object();
    map.put("this", new PageContextTest(pageContext));
    assertNotNull(CFUtils.findPageContext(map));
    assertSame(pageContext, CFUtils.findPageContext(map));
  }


  @Test
  void isCfClass() {
    assertFalse(CFUtils.isCfClass("any/random/class"));
    assertFalse(CFUtils.isCfClass("any.random.class"));
    assertFalse(CFUtils.isCfClass("anyclass"));

    assertTrue(CFUtils.isCfClass("cf123546123$funcFINDFILES"));
    assertTrue(CFUtils.isCfClass("tests.now_cfm$cf"));
  }

  @Test
  void guessSource() {
    assertNull(CFUtils.guessSource("cf123546123$funcFINDFILES"));
    assertEquals("tests/now.cfm", CFUtils.guessSource("tests.now_cfm$cf"));
  }

  @Test
  void loadCfTracepoints() {
    final Set<TracePointConfig> tracePointConfigs = CFUtils.loadCfTracepoints("some/file.cfm",
        Collections.singletonMap("cfm", new MockTracepointConfig("some/file.cfm")));
    assertEquals(1, tracePointConfigs.size());
  }

  public static class CFEvaluatorTarget {

    public String Evaluate(String expr) {
      return CFEvaluatorTarget.class.getName();
    }
  }


  public static class CFEvaluatorTarget2 {

    public String Evaluate(Object expr) {
      return CFEvaluatorTarget2.class.getName();
    }
  }


  public static class CfUdfTest extends UDFMethod {

    public CfUdfTest(final String key) {
      super(key);
    }
  }


  public static class PageContextTest {

    private final Object pageContext;


    public PageContextTest(final Object pageContext) {
      this.pageContext = pageContext;
    }
  }
}