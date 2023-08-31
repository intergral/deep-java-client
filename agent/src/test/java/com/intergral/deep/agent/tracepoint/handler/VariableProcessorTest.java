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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.push.PushUtils;
import com.intergral.deep.agent.tracepoint.handler.VariableProcessor.VariableResponse;
import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.test.target.VariableTypeTarget;
import com.intergral.deep.tests.snapshot.SnapshotUtils;
import com.intergral.deep.tests.snapshot.SnapshotUtils.IVariableScan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VariableProcessorTest {

  private VariableProcessor variableProcessor;

  @BeforeEach
  void setUp() {
    variableProcessor = new VariableProcessor() {
    };
    variableProcessor.configureSelf(new ArrayList<>());

  }

  @Test
  void variableTypes() {
    final List<VariableID> variableIDS = processVars(Collections.singletonMap("this", new VariableTypeTarget()));
    assertNotNull(variableIDS);

    final Collection<com.intergral.deep.proto.tracepoint.v1.VariableID> localVars = PushUtils.covertVariables(variableIDS);
    final Map<String, Variable> lookup = PushUtils.convertVarLookup(variableProcessor.varLookup);
    final IVariableScan aThis = SnapshotUtils.findVarByName("this", localVars, lookup);
    assertNotNull(aThis);
    assertEquals("VariableTypeTarget{}", aThis.variable().getValue());

    final IVariableScan VariableSuperTesti = SnapshotUtils.findVarByName("VariableSuperTest.i", aThis.variable().getChildrenList(), lookup);
    assertTrue(VariableSuperTesti.found());
    assertEquals("9", VariableSuperTesti.variable().getValue());
    assertArrayEquals(
        Arrays.stream(new String[]{"public", "static"}).sorted().toArray(),
        Arrays.stream(VariableSuperTesti.variableId().getModifiersList().toArray(new String[0])).sorted().toArray());
    assertEquals("i", VariableSuperTesti.variableId().getOriginalName());

    final IVariableScan VariableSuperSuperTesti = SnapshotUtils.findVarByName("VariableSuperSuperTest.i",
        aThis.variable().getChildrenList(), lookup);
    assertTrue(VariableSuperSuperTesti.found());
    assertEquals("11", VariableSuperSuperTesti.variable().getValue());
    assertArrayEquals(
        Arrays.stream(new String[]{"public", "static"}).sorted().toArray(),
        Arrays.stream(VariableSuperSuperTesti.variableId().getModifiersList().toArray(new String[0])).sorted().toArray());
    assertEquals("i", VariableSuperSuperTesti.variableId().getOriginalName());

    final IVariableScan VariableSuperTestl = SnapshotUtils.findVarByName("VariableSuperTest.l", aThis.variable().getChildrenList(), lookup);
    assertTrue(VariableSuperTestl.found());
    assertEquals("8", VariableSuperTestl.variable().getValue());
    assertArrayEquals(new String[]{"static"}, VariableSuperTestl.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan VariableSuperTestd = SnapshotUtils.findVarByName("VariableSuperTest.d", aThis.variable().getChildrenList(), lookup);
    assertTrue(VariableSuperTestd.found());
    assertEquals("9.8", VariableSuperTestd.variable().getValue());
    assertArrayEquals(Arrays.stream(new String[]{"private", "static"}).sorted().toArray(),
        VariableSuperTestd.variableId().getModifiersList().toArray(Arrays.stream(new String[0]).sorted().toArray()));

    final IVariableScan VariableSuperTestf = SnapshotUtils.findVarByName("VariableSuperTest.f", aThis.variable().getChildrenList(), lookup);
    assertTrue(VariableSuperTestf.found());
    assertEquals("8.8", VariableSuperTestf.variable().getValue());
    assertArrayEquals(Arrays.stream(new String[]{"protected", "static"}).sorted().toArray(),
        Arrays.stream(VariableSuperTestf.variableId().getModifiersList().toArray(new String[0])).sorted().toArray());

    final IVariableScan i = SnapshotUtils.findVarByName("i", aThis.variable().getChildrenList(), lookup);
    assertTrue(i.found());
    assertEquals("1", i.variable().getValue());
    assertArrayEquals(
        Arrays.stream(new String[]{"public", "static"}).sorted().toArray(),
        Arrays.stream(i.variableId().getModifiersList().toArray(new String[0])).sorted().toArray());

    final IVariableScan l = SnapshotUtils.findVarByName("l", aThis.variable().getChildrenList(), lookup);
    assertTrue(l.found());
    assertEquals("2", l.variable().getValue());
    assertArrayEquals(new String[]{"static"}, l.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan d = SnapshotUtils.findVarByName("d", aThis.variable().getChildrenList(), lookup);
    assertTrue(d.found());
    assertEquals("1.2", d.variable().getValue());
    assertArrayEquals(Arrays.stream(new String[]{"private", "static"}).sorted().toArray(),
        d.variableId().getModifiersList().toArray(Arrays.stream(new String[0]).sorted().toArray()));

    final IVariableScan f = SnapshotUtils.findVarByName("f", aThis.variable().getChildrenList(), lookup);
    assertTrue(f.found());
    assertEquals("2.2", f.variable().getValue());
    assertArrayEquals(Arrays.stream(new String[]{"protected", "static"}).sorted().toArray(),
        Arrays.stream(f.variableId().getModifiersList().toArray(new String[0])).sorted().toArray());

    final IVariableScan str = SnapshotUtils.findVarByName("str", aThis.variable().getChildrenList(), lookup);
    assertTrue(str.found());
    assertTrue(str.variable().getValue().startsWith("str with a very large value will be truncated, needs to be over 1024 by default"));
    assertEquals(1024, str.variable().getValue().length());
    assertTrue(str.variable().hasTruncated());
    assertArrayEquals(new String[]{"public"}, str.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan nul = SnapshotUtils.findVarByName("nul", aThis.variable().getChildrenList(), lookup);
    assertTrue(nul.found());
    assertEquals("null", nul.variable().getValue());
    assertArrayEquals(new String[]{}, nul.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan object = SnapshotUtils.findVarByName("object", aThis.variable().getChildrenList(), lookup);
    assertTrue(object.found());
    assertEquals("java.lang.Object", object.variable().getValue().split("@")[0]);
    assertArrayEquals(new String[]{"private", "final"}, object.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan arry = SnapshotUtils.findVarByName("arry", aThis.variable().getChildrenList(), lookup);
    assertTrue(arry.found());
    assertEquals("Array of length: 2", arry.variable().getValue());
    assertArrayEquals(new String[]{"protected"}, arry.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan list = SnapshotUtils.findVarByName("list", aThis.variable().getChildrenList(), lookup);
    assertTrue(list.found());
    assertEquals("ArrayList of size: 0", list.variable().getValue());
    assertArrayEquals(new String[]{"private", "volatile"}, list.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan set = SnapshotUtils.findVarByName("set", aThis.variable().getChildrenList(), lookup);
    assertTrue(set.found());
    assertEquals("HashSet of size: 2", set.variable().getValue());
    assertArrayEquals(new String[]{"private", "transient"}, set.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan simplemap = SnapshotUtils.findVarByName("simplemap", aThis.variable().getChildrenList(), lookup);
    assertTrue(simplemap.found());
    assertEquals("HashMap of size: 1", simplemap.variable().getValue());
    assertArrayEquals(new String[]{"private"}, simplemap.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan complexMap = SnapshotUtils.findVarByName("complexMap", aThis.variable().getChildrenList(), lookup);
    assertTrue(complexMap.found());
    assertEquals("VariableTypeTarget$1 of size: 3", complexMap.variable().getValue());
    assertArrayEquals(new String[]{"private"}, complexMap.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan iter = SnapshotUtils.findVarByName("iter", aThis.variable().getChildrenList(), lookup);
    assertTrue(iter.found());
    assertEquals("Iterator of type: HashMap$EntryIterator", iter.variable().getValue());
    assertArrayEquals(new String[]{"private"}, iter.variableId().getModifiersList().toArray(new String[0]));

    final IVariableScan someObj = SnapshotUtils.findVarByName("someObj", aThis.variable().getChildrenList(), lookup);
    assertTrue(someObj.found());
    assertEquals("VariableTypeTarget{}", someObj.variable().getValue());
    assertArrayEquals(new String[]{"private"}, someObj.variableId().getModifiersList().toArray(new String[0]));
  }


  protected List<VariableID> processVars(final Map<String, Object> variables) {
    final List<VariableID> frameVars = new ArrayList<>();

    final Node.IParent frameParent = frameVars::add;

    final Set<Node> initialNodes = variables.entrySet()
        .stream()
        .map(stringObjectEntry -> new Node(new Node.NodeValue(stringObjectEntry.getKey(),
            stringObjectEntry.getValue()), frameParent)).collect(Collectors.toSet());

    Node.breadthFirstSearch(new Node(null, new HashSet<>(initialNodes), frameParent),
        this::processNode);

    return frameVars;
  }

  protected boolean processNode(final Node node) {
    if (!variableProcessor.checkVarCount()) {
      // we have exceeded the var count, so do not continue
      return false;
    }

    final Node.NodeValue value = node.getValue();
    if (value == null) {
      // this node has no value, continue with children
      return true;
    }

    // process this node variable
    final VariableResponse processResult = variableProcessor.processVariable(value);
    final VariableID variableId = processResult.getVariableId();

    // add the result to the parent - this maintains the hierarchy in the var look up
    node.getParent().addChild(variableId);

    if (value.getValue() != null && processResult.processChildren()) {
      final Set<Node> childNodes = variableProcessor.processChildNodes(variableId, value.getValue(),
          node.depth());
      node.addChildren(childNodes);
    }
    return true;
  }
}