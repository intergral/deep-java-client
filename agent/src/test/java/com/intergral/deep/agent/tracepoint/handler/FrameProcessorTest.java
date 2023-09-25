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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.plugin.EvaluationException;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.evaluator.EvaluatorService;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.agent.types.snapshot.WatchResult;
import com.intergral.deep.test.MockTracepointConfig;
import com.intergral.deep.test.target.ConditionTarget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FrameProcessorTest {

  private final Settings settings = Mockito.mock(Settings.class);
  private final IEvaluator evaluator = EvaluatorService.createEvaluator();
  private final Collection<TracePointConfig> tracepoints = new ArrayList<>();
  private FrameProcessor frameProcessor;

  @BeforeEach
  void setUp() {
    Mockito.when(settings.getResource()).thenReturn(Resource.DEFAULT);
    tracepoints.clear();
    frameProcessor = new FrameProcessor(settings, evaluator, Collections.singletonMap("this", new ConditionTarget()), tracepoints,
        Utils.currentTimeNanos(), Thread.currentThread().getStackTrace());
  }

  @Test
  void canCollect() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig();
    tracepoints.add(tracepointConfig);

    assertTrue(frameProcessor.canCollect());
    tracepointConfig.fired(Utils.currentTimeNanos()[0]);

    assertFalse(frameProcessor.canCollect());
  }

  @Test
  void canCollect_withCondition() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig().withArg(TracePointConfig.CONDITION, "this.i == 101");
    tracepoints.add(tracepointConfig);

    assertFalse(frameProcessor.canCollect());
    tracepointConfig.withArg(TracePointConfig.CONDITION, "this.i == 100");

    assertTrue(frameProcessor.canCollect());
  }

  @Test
  void willCollect() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig();
    tracepoints.add(tracepointConfig);
    assertTrue(frameProcessor.canCollect());
    frameProcessor.configureSelf();

    final Collection<EventSnapshot> collect = frameProcessor.collect();
    assertEquals(1, collect.size());
  }

  @Test
  void willCollect_2() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig();
    tracepoints.add(tracepointConfig);
    tracepoints.add(new MockTracepointConfig());
    assertTrue(frameProcessor.canCollect());
    frameProcessor.configureSelf();

    final Collection<EventSnapshot> collect = frameProcessor.collect();
    assertEquals(2, collect.size());
  }

  @Test
  void willCollect_watches() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig().withWatches("this.i", "this.i - 10");
    tracepoints.add(tracepointConfig);
    assertTrue(frameProcessor.canCollect());
    frameProcessor.configureSelf();

    final Collection<EventSnapshot> collect = frameProcessor.collect();
    assertEquals(1, collect.size());
    final EventSnapshot next = collect.iterator().next();
    final ArrayList<WatchResult> watches = next.getWatches();
    assertEquals(2, watches.size());

    final WatchResult iWatch;
    final WatchResult i10Watch;

    // order is not determined
    if (watches.get(0).expression().equals("this.i")) {
      iWatch = watches.get(0);
      i10Watch = watches.get(1);
    } else {
      iWatch = watches.get(1);
      i10Watch = watches.get(0);
    }

    assertEquals("this.i", iWatch.expression());
    assertNotNull(iWatch.goodResult());
    assertEquals("100", next.getVarLookup().get(iWatch.goodResult().getId()).getValString());

    assertEquals("this.i - 10", i10Watch.expression());
    assertNotNull(i10Watch.goodResult());
    assertEquals("90.0", next.getVarLookup().get(i10Watch.goodResult().getId()).getValString());
  }

  @Test
  void willCollect_watches_2() {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig().withWatches("this.i", "10 - this.i");
    tracepoints.add(tracepointConfig);
    assertTrue(frameProcessor.canCollect());
    frameProcessor.configureSelf();

    final Collection<EventSnapshot> collect = frameProcessor.collect();
    assertEquals(1, collect.size());
    final EventSnapshot next = collect.iterator().next();
    final ArrayList<WatchResult> watches = next.getWatches();
    assertEquals(2, watches.size());

    final WatchResult iWatch;
    final WatchResult i10Watch;

    // order is not determined
    if (watches.get(0).expression().equals("this.i")) {
      iWatch = watches.get(0);
      i10Watch = watches.get(1);
    } else {
      iWatch = watches.get(1);
      i10Watch = watches.get(0);
    }

    assertEquals("this.i", iWatch.expression());
    assertNotNull(iWatch.goodResult());
    assertEquals("100", next.getVarLookup().get(iWatch.goodResult().getId()).getValString());

    assertEquals("10 - this.i", i10Watch.expression());
    assertNotNull(i10Watch.goodResult());
    assertEquals("-90.0", next.getVarLookup().get(i10Watch.goodResult().getId()).getValString());
  }

  @Test
  void willEvaluate() throws EvaluationException {
    assertEquals("100", frameProcessor.evaluateExpression("this.i"));
  }

  @Test
  void reflection() {
    assertNotNull(frameProcessor.reflectionService());
  }
}