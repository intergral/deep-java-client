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

package com.intergral.deep.test;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.agent.types.snapshot.StackFrame;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.agent.types.snapshot.WatchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MockEventSnapshot extends EventSnapshot {

  public MockEventSnapshot() {
    super(new TracePointConfig("tp-1", "/some/file/path.py", 123, new HashMap<>(), new ArrayList<>(), new ArrayList<>()), 1011L, Resource.DEFAULT,
        new ArrayList<>(), new HashMap<>());
  }

  public MockEventSnapshot withFrames() {
    this.getFrames().add(new StackFrame("file_name_1", 4321, "class_name_1", "methodName_1", true, true, new ArrayList<>(),
        null, -1));
    this.getFrames().add(new StackFrame("file_name_2", 321, "class_name_2", "methodName_2", false, true, new ArrayList<>(),
        null, -1));
    this.getFrames().add(new StackFrame("file_name_3", 21, "class_name_3", "methodName_3", true, false, new ArrayList<>(),
        null, -1));

    final VariableID someLocalVar = new VariableID("var-id-1", "someLocalVar", new HashSet<>(), null);
    final HashSet<String> modifiers = new HashSet<>();
    modifiers.add("private");
    final VariableID someLocalVar2 = new VariableID("var-id-2", "someLocalVar2", modifiers, null);
    final VariableID someLocalVar3 = new VariableID("var-id-3", "someLocalVar3", new HashSet<>(), "someOtherName");
    final ArrayList<VariableID> frameVariables = new ArrayList<>();
    frameVariables.add(someLocalVar);
    frameVariables.add(someLocalVar2);
    frameVariables.add(someLocalVar3);
    this.getFrames().add(new StackFrame("file_name_4", 1, "class_name_4", "methodName_4", false, false, frameVariables,
        null, -1));
    return this;
  }

  public MockEventSnapshot withAttributes() {
    final HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("long", 123L);
    attributes.put("int", 123);
    attributes.put("float", 3.21F);
    attributes.put("double", 1.23D);
    attributes.put("boolean", false);
    attributes.put("string", "123l");
    mergeAttributes(Resource.create(attributes));
    return this;
  }

  public MockEventSnapshot withWatches() {
    getWatches().add(new WatchResult("error", "this is an error"));
    getWatches().add(new WatchResult("good", new VariableID("some-var", "withName", new HashSet<>(), null)));
    return this;
  }

  public MockEventSnapshot withVariables() {
    getVarLookup().put("1", new Variable("string", "value_1", "123124", false));
    getVarLookup().put("2", new Variable("string", "value_2", "123124", true));
    return this;
  }
}
