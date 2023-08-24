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

package com.intergral.deep.agent.tracepoint.inst.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.logging.Logger;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.push.PushUtils;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.handler.Callback;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.test.MockTracepointConfig;
import com.intergral.deep.test.target.BPTestTarget;
import com.intergral.deep.tests.inst.ByteClassLoader;
import com.intergral.deep.tests.snapshot.SnapshotUtils;
import com.intergral.deep.tests.snapshot.SnapshotUtils.IVariableScan;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * This test can be used to play with visitor, it uses the {@link BPTestTarget} class to install tracepoints into
 * <p>
 * To run this use the 'VisitorTest' saved config for idea, or add
 * {@code -Ddeep.callback.class=com.intergral.deep.agent.tracepoint.handler.Callback} to the test runner args
 * <p>
 * WARNING: The line numbers used in this test are important, they must match the line numbers on the test target classes!!
 */
class VisitorTest {

  private final Settings settings = Mockito.mock(Settings.class);
  private final TracepointConfigService tracepointConfigService = Mockito.mock(TracepointConfigService.class);
  private final PushService pushService = Mockito.mock(PushService.class);
  private final Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
  private final String disPath = Paths.get(Paths.get(".").normalize().toAbsolutePath().getParent().toString(), "dispath").toString();

  private final AtomicReference<TracePointConfig> tracepointRef = new AtomicReference<>();
  private TracepointInstrumentationService instrumentationService;

  @BeforeEach
  void setUp() {
    // set up logging to help with debugging tests
    Mockito.when(settings.getSettingAs("logging.level", Level.class)).thenReturn(Level.FINEST);
    Logger.configureLogging(settings);

    Mockito.when(settings.getResource()).thenReturn(Resource.DEFAULT);

    // for these tests we do not care about these (they are tested in the TracepointInstrumentationServiceTest)
    // we just need them to pass, as we manage the instrumentation ourselves.
    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[0]);
    Mockito.when(instrumentation.isModifiableClass(Mockito.any())).thenReturn(true);

    // these settings are needed for jsp transforms
    Mockito.when(settings.getSettingAs("jsp.packages", List.class)).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.suffix", String.class)).thenReturn("_jsp");

    Mockito.when(tracepointConfigService.loadTracepointConfigs(Mockito.any())).thenAnswer(invocationOnMock -> {
      final TracePointConfig tracePointConfig = tracepointRef.get();
      if (tracePointConfig == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(tracePointConfig);
    });

    instrumentationService = new TracepointInstrumentationService(instrumentation, settings);

    Callback.init(settings, tracepointConfigService, pushService);
  }

  // test a line within the constructor
  @Test
  void constructor() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 21);
    tracepointRef.set(tracepointConfig);

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);

    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed, name + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance("my test", 4);
    assertNotNull(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    assertTrue(scanResult.found());
    assertEquals("my test", scanResult.variable().getValue());
  }

  // test the last line of the constructor closing brace
  @Test
  void constructor_end_line() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 23);
    tracepointRef.set(tracepointConfig);

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed, name + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance("my test", 4);
    assertNotNull(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // the parameter to the constructor should be available
    assertTrue(scanResult.found());
    assertEquals("my test", scanResult.variable().getValue());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name'
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("my test", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namemy test", superName.variable().getValue());
  }

  // test first line in constructor (super call)
  @Test
  void constructor_start_line() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 20);
    tracepointRef.set(tracepointConfig);

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed, name + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance("my test", 4);
    assertNotNull(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // the parameter to the constructor should be available
    assertTrue(scanResult.found());
    assertEquals("my test", scanResult.variable().getValue());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' but it should not have a value as our TP is before this is set
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namemy test", superName.variable().getValue());
  }
}