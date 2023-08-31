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

package com.intergral.deep.agent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;

import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.agent.tracepoint.handler.Callback;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class AgentImplTest {

  @Test
  void awaitLatch() throws InterruptedException {
    try (MockedStatic<Callback> callback = Mockito.mockStatic(Callback.class, "init")) {
      assertThrows(IllegalStateException.class, AgentImpl::loadDeepAPI);
      AgentImpl.startup(Mockito.mock(Instrumentation.class), new HashMap<>());
      final Object o = AgentImpl.awaitLoadAPI();
      assertNotNull(o);

      final IDeepHook deepHook = (IDeepHook) o;

      assertNotNull(deepHook.deepService());
      assertNotNull(deepHook.reflectionService());

      callback.verify(() -> Callback.init(Mockito.any(), Mockito.any(), Mockito.any()), times(1));
    }
  }
}