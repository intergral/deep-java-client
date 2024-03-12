/*
 *     Copyright (C) 2024  Intergral GmbH
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

package com.intergral.deep.plugin.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.intergral.deep.agent.api.plugin.EvaluationException;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.resource.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CFPluginTest {

  @Test
  void canDecorate() throws EvaluationException {
    final CFPlugin cfPlugin = new CFPlugin();
    final ISnapshotContext mock = Mockito.mock(ISnapshotContext.class);

    final Resource decorate = cfPlugin.decorate(null, mock);
    assertNotNull(decorate);
    assertEquals("10", decorate.getAttributes().get("cf_version"));
    assertNull(decorate.getAttributes().get("app_name"));

    Mockito.verify(mock).evaluateExpression("APPLICATION.applicationname");
  }

  @Test
  void canHandleEvaluateDecorate() throws EvaluationException {
    final CFPlugin cfPlugin = new CFPlugin();

    final ISnapshotContext mock = Mockito.mock(ISnapshotContext.class);

    Mockito.when(mock.evaluateExpression("APPLICATION.applicationname")).thenReturn("app_name");

    final Resource decorate = cfPlugin.decorate(null, mock);
    assertNotNull(decorate);
    assertEquals("10", decorate.getAttributes().get("cf_version"));
    assertEquals("app_name", decorate.getAttributes().get("app_name"));

    Mockito.verify(mock).evaluateExpression("APPLICATION.applicationname");
  }

  @Test
  void canHandleEvaluateFailedDecorate() throws EvaluationException {
    final CFPlugin cfPlugin = new CFPlugin();

    final ISnapshotContext mock = Mockito.mock(ISnapshotContext.class);

    Mockito.when(mock.evaluateExpression("APPLICATION.applicationname")).thenThrow(new RuntimeException("test exception"));

    final Resource decorate = cfPlugin.decorate(null, mock);
    assertNotNull(decorate);
    assertEquals("10", decorate.getAttributes().get("cf_version"));
    assertNull(decorate.getAttributes().get("app_name"));

    Mockito.verify(mock).evaluateExpression("APPLICATION.applicationname");
  }

  @Test
  void isActive() {
    assertFalse(new CFPlugin().isActive());
  }
}