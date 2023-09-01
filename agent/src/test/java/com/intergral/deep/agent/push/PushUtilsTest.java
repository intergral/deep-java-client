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

package com.intergral.deep.agent.push;

import static com.intergral.deep.tests.AssertUtils.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.test.MockEventSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PushUtilsTest {

  @Test
  void canProcessSnapshot() {
    final MockEventSnapshot eventSnapshot = new MockEventSnapshot();
    final Snapshot snapshot = PushUtils.convertToGrpc(eventSnapshot);
    assertEquals(eventSnapshot.getID(), snapshot.getID().toStringUtf8());

    assertEquals("tp-1", snapshot.getTracepoint().getID());
    assertEquals("/some/file/path.py", snapshot.getTracepoint().getPath());
    assertEquals(123, snapshot.getTracepoint().getLineNumber());
  }

  @Test
  void canHandleFrames() {
    final MockEventSnapshot mockEventSnapshot = new MockEventSnapshot().withFrames();
    final Snapshot snapshot = PushUtils.convertToGrpc(mockEventSnapshot);

    assertEquals("class_name_1", snapshot.getFrames(0).getClassName());
    assertEquals("class_name_2", snapshot.getFrames(1).getClassName());
    assertEquals("class_name_3", snapshot.getFrames(2).getClassName());
    assertEquals("class_name_4", snapshot.getFrames(3).getClassName());

    assertTrue(snapshot.getFrames(0).getNativeFrame());
    assertTrue(snapshot.getFrames(0).getAppFrame());

    assertFalse(snapshot.getFrames(3).getNativeFrame());
    assertFalse(snapshot.getFrames(3).getAppFrame());

    assertEquals(3, snapshot.getFrames(3).getVariablesCount());

    assertEquals("someLocalVar", snapshot.getFrames(3).getVariables(0).getName());
    assertEquals("private", snapshot.getFrames(3).getVariables(1).getModifiers(0));
    assertEquals("someOtherName", snapshot.getFrames(3).getVariables(2).getOriginalName());
  }

  @Test
  void canConvertAllTypes() {
    final MockEventSnapshot mockEventSnapshot = new MockEventSnapshot().withAttributes();
    final Snapshot snapshot = PushUtils.convertToGrpc(mockEventSnapshot);

    final List<KeyValue> attributesList = new ArrayList<>(snapshot.getAttributesList());
    attributesList.remove(assertContains(attributesList, (item) -> item.getValue().getIntValue() == 123L));
    attributesList.remove(assertContains(attributesList, (item) -> item.getValue().getIntValue() == 123));
    attributesList.remove(assertContains(attributesList, (item) -> item.getValue().getDoubleValue() == 3.21F));
    attributesList.remove(assertContains(attributesList, (item) -> item.getValue().getDoubleValue() == 1.23D));
    attributesList.remove(assertContains(attributesList, (item) -> !item.getValue().getBoolValue()));
    attributesList.remove(assertContains(attributesList, (item) -> "123l".equals(item.getValue().getStringValue())));

    assertEquals(0, attributesList.size());
  }

  @Test
  void canConvertWatches() {
    final MockEventSnapshot mockEventSnapshot = new MockEventSnapshot().withWatches();
    final Snapshot snapshot = PushUtils.convertToGrpc(mockEventSnapshot);

    assertEquals("error", snapshot.getWatches(0).getExpression());
    assertEquals("this is an error", snapshot.getWatches(0).getErrorResult());
    assertEquals("good", snapshot.getWatches(1).getExpression());
    assertEquals("withName", snapshot.getWatches(1).getGoodResult().getName());
  }

  @Test
  void canConvertVariables() {
    final MockEventSnapshot mockEventSnapshot = new MockEventSnapshot().withVariables();
    final Snapshot snapshot = PushUtils.convertToGrpc(mockEventSnapshot);

    assertEquals("value_1", snapshot.getVarLookupOrThrow("1").getValue());
    assertFalse(snapshot.getVarLookupOrThrow("1").getTruncated());
    assertEquals("value_2", snapshot.getVarLookupOrThrow("2").getValue());
    assertTrue(snapshot.getVarLookupOrThrow("2").getTruncated());
  }
}