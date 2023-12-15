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

package com.intergral.deep.agent.tracepoint;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TracepointConfigServiceTest {

  private TracepointInstrumentationService instrumentationService;
  private TracepointConfigService tracepointConfigService;

  @BeforeEach
  void setUp() {
    instrumentationService = Mockito.mock(TracepointInstrumentationService.class);
    tracepointConfigService = new TracepointConfigService(instrumentationService);
  }

  @Test
  void noChange() {
    tracepointConfigService.configUpdate(1010110, "hash", Collections.emptyList());
    assertEquals(1010110, tracepointConfigService.lastUpdate());
    assertEquals("hash", tracepointConfigService.currentHash());
    Mockito.verify(instrumentationService, Mockito.times(1)).processBreakpoints(Mockito.anyCollection());

    tracepointConfigService.noChange(202020);
    assertEquals(202020, tracepointConfigService.lastUpdate());
    assertEquals("hash", tracepointConfigService.currentHash());
    Mockito.verify(instrumentationService, Mockito.times(1)).processBreakpoints(Mockito.anyCollection());
  }

  @Test
  void customCallsUpdate() {
    // add a custom tracepoint will call instrumentation update
    final TracePointConfig tracePointConfig = tracepointConfigService.addCustom("/path", 123, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList());

    final ArgumentCaptor<Collection<TracePointConfig>> captor = ArgumentCaptor.forClass(Collection.class);

    Mockito.verify(instrumentationService, Mockito.times(1)).processBreakpoints(captor.capture());

    final Collection<TracePointConfig> value = captor.getValue();
    assertEquals(1, value.size());
    final TracePointConfig next = value.iterator().next();

    assertEquals(tracePointConfig.getId(), next.getId());

    // remove a custom tracepoint will call instrumentation update
    tracepointConfigService.removeCustom(tracePointConfig);

    Mockito.verify(instrumentationService, Mockito.times(2)).processBreakpoints(captor.capture());

    final Collection<TracePointConfig> captorValue = captor.getValue();
    assertEquals(0, captorValue.size());

    // remove a tp that doesn't exist will not call instrumentation update
    tracepointConfigService.removeCustom(tracePointConfig);

    Mockito.verify(instrumentationService, Mockito.times(2)).processBreakpoints(Mockito.anyCollection());
  }

  @Test
  void canLoadTracepointConfigs() {

    final TracePointConfig tracePointConfig1 = tracepointConfigService.addCustom("/path", 123, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList());
    final TracePointConfig tracePointConfig2 = tracepointConfigService.addCustom("/path", 123, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList());
    final TracePointConfig tracePointConfig3 = tracepointConfigService.addCustom("/path", 123, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList());

    {
      final Collection<TracePointConfig> tracePointConfigs = tracepointConfigService.loadTracepointConfigs(
          Collections.singletonList(tracePointConfig1.getId()));
      assertEquals(1, tracePointConfigs.size());
      assertEquals(tracePointConfig1.getId(), tracePointConfigs.iterator().next().getId());
    }

    {
      final Collection<TracePointConfig> tracePointConfigs = tracepointConfigService.loadTracepointConfigs(
          Collections.singletonList(tracePointConfig2.getId()));
      assertEquals(1, tracePointConfigs.size());
      assertEquals(tracePointConfig2.getId(), tracePointConfigs.iterator().next().getId());
    }

    {
      final Collection<TracePointConfig> tracePointConfigs = tracepointConfigService.loadTracepointConfigs(
          Arrays.asList(tracePointConfig2.getId(), tracePointConfig3.getId()));
      assertEquals(2, tracePointConfigs.size());
      assertArrayEquals(tracePointConfigs.stream().map(TracePointConfig::getId).toArray(),
          new String[]{tracePointConfig2.getId(), tracePointConfig3.getId()});
    }
  }

  @Test
  void configUpdate() {
    // TP config is passed to instrumentation
    tracepointConfigService.configUpdate(10101, "hash",
        Collections.singletonList(
            new TracePointConfig("some-id", "path", 123, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList())));

    final ArgumentCaptor<Collection<TracePointConfig>> captor = ArgumentCaptor.forClass(Collection.class);

    Mockito.verify(instrumentationService, Mockito.times(1)).processBreakpoints(captor.capture());

    {
      final Collection<TracePointConfig> value = captor.getValue();
      assertEquals(1, value.size());
      assertEquals("some-id", value.iterator().next().getId());
    }

    // custom and config are passed to instrumentation
    final TracePointConfig customTp = tracepointConfigService.addCustom("path", 123, Collections.emptyMap(),
        Collections.emptyList(), Collections.emptyList());

    Mockito.verify(instrumentationService, Mockito.times(2)).processBreakpoints(captor.capture());

    {
      final Collection<TracePointConfig> tracePointConfigs = captor.getValue();
      assertEquals(2, tracePointConfigs.size());

      assertArrayEquals(tracePointConfigs.stream().map(TracePointConfig::getId).toArray(),
          new String[]{"some-id", customTp.getId()});
    }

    //custom remains after update
    tracepointConfigService.configUpdate(10101, "hash",
        Collections.singletonList(
            new TracePointConfig("some-id", "path", 123, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList())));

    Mockito.verify(instrumentationService, Mockito.times(3)).processBreakpoints(captor.capture());

    {
      final Collection<TracePointConfig> tracePointConfigs = captor.getValue();
      assertEquals(2, tracePointConfigs.size());

      assertArrayEquals(tracePointConfigs.stream().map(TracePointConfig::getId).toArray(),
          new String[]{"some-id", customTp.getId()});
    }
    // config remains after custom removed
    tracepointConfigService.removeCustom(customTp);

    Mockito.verify(instrumentationService, Mockito.times(4)).processBreakpoints(captor.capture());

    {
      final Collection<TracePointConfig> tracePointConfigs = captor.getValue();
      assertEquals(1, tracePointConfigs.size());

      assertArrayEquals(tracePointConfigs.stream().map(TracePointConfig::getId).toArray(),
          new String[]{"some-id"});
    }
  }
}