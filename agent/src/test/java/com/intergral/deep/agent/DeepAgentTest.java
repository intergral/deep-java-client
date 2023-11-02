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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

import com.intergral.deep.agent.api.DeepVersion;
import com.intergral.deep.agent.api.plugin.IPlugin.IPluginRegistration;
import com.intergral.deep.agent.api.tracepoint.ITracepoint;
import com.intergral.deep.agent.api.tracepoint.ITracepoint.ITracepointRegistration;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.handler.Callback;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DeepAgentTest {

  private final Settings settings = Mockito.mock(Settings.class);
  private final TracepointInstrumentationService tracepointInstrumentationService = Mockito.mock(TracepointInstrumentationService.class);
  private DeepAgent deepAgent;

  @BeforeEach
  void setUp() {

    Mockito.when(settings.getSettingAs("poll.timer", Integer.class)).thenReturn(1010);
    Mockito.when(settings.getSettingAs(Mockito.anyString(), Mockito.eq(String.class))).thenReturn("");
    Mockito.when(settings.getSettingAs(Mockito.anyString(), Mockito.eq(Boolean.class))).thenReturn(false);
    Mockito.when(settings.getServiceHost()).thenReturn("localhost");

    try (MockedStatic<Callback> callback = Mockito.mockStatic(Callback.class, "init")) {
      deepAgent = new DeepAgent(settings, tracepointInstrumentationService);
      callback.verify(() -> Callback.init(Mockito.any(), Mockito.any(), Mockito.any()), times(1));
    }
  }

  @Test
  void start_shouldSetPluginsAndResource() throws IOException {

    // find a free port
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    Mockito.when(settings.getServicePort()).thenReturn(port);

    final Server server = ServerBuilder.forPort(port).build();

    server.start();

    deepAgent.start();

    server.shutdownNow();
    deepAgent.shutdown();

    Mockito.verify(settings).setPlugins(Mockito.anyCollection());
    Mockito.verify(settings).setResource(Mockito.any());

  }

  @Test
  void registerPlugin() {
    final IPluginRegistration iPluginRegistration = deepAgent.registerPlugin((settings, snapshot) -> null);

    assertNotNull(iPluginRegistration.get());
    assertFalse(iPluginRegistration.isAuthProvider());
    assertFalse(iPluginRegistration.isTracepointLogger());
    assertFalse(iPluginRegistration.isResourceProvider());

    Mockito.verify(settings, times(1)).addPlugin(Mockito.any());

    iPluginRegistration.unregister();

    Mockito.verify(settings, times(1)).removePlugin(Mockito.any());
  }

  @Test
  void registerTracepoint() {
    final ITracepointRegistration iTracepointRegistration = deepAgent.registerTracepoint("some/path", 123);

    final ITracepoint iTracepoint = iTracepointRegistration.get();
    assertEquals("some/path", iTracepoint.path());
    assertEquals(123, iTracepoint.line());
    assertNotNull(iTracepoint.id());
    assertNotNull(iTracepoint.watches());
    assertNotNull(iTracepoint.args());

    iTracepointRegistration.unregister();
  }

  @Test
  void isEnabled() {
    Mockito.when(settings.isActive()).thenReturn(false).thenReturn(false).thenReturn(true);

    assertFalse(deepAgent.isEnabled());

    deepAgent.setEnabled(true);

    assertTrue(deepAgent.isEnabled());
  }

  @Test
  void getVersion() {
    assertEquals(DeepVersion.VERSION, deepAgent.getVersion());
  }
}