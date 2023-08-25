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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
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

  private final AtomicReference<Collection<TracePointConfig>> tracepointRef = new AtomicReference<>();
  private final AtomicReference<URL> cfUrl = new AtomicReference<>();
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
    Mockito.when(settings.getAsList("jsp.packages")).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.packages", List.class)).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.suffix", String.class)).thenReturn("_jsp");

    Mockito.when(tracepointConfigService.loadTracepointConfigs(Mockito.any())).thenAnswer(invocationOnMock -> {
      final Collection<TracePointConfig> tracePointConfig = tracepointRef.get();
      if (tracePointConfig == null) {
        return Collections.emptyList();
      }
      return tracePointConfig;
    });

    instrumentationService = new TracepointInstrumentationService(instrumentation, settings) {
      @Override
      protected URL getLocation(final ProtectionDomain protectionDomain) {
        final URL url = cfUrl.get();
        if (url == null) {
          return super.getLocation(protectionDomain);
        }
        return url;
      }
    };

    Callback.init(settings, tracepointConfigService, pushService);
  }

  // test a line within the constructor
  @Test
  void constructor() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 21);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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

    assertEquals("<init>", snapshot.getFrames(0).getMethodName());
  }

  // test the last line of the constructor closing brace
  @Test
  void constructor_end_line() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 23);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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

    assertEquals("<init>", snapshot.getFrames(0).getMethodName());
  }

  // test first line in constructor (super call)
  @Test
  void constructor_start_line() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 20);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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

    assertEquals("<init>", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void setName() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 51);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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

    final Method setName = aClass.getDeclaredMethod("setName", String.class);
    setName.invoke(myTest, "something");

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // the parameter to the constructor should be available
    assertTrue(scanResult.found());
    assertEquals("something", scanResult.variable().getValue());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("my test", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namemy test", superName.variable().getValue());

    assertEquals("setName", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void getName_null_return() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 44);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("getName");
    setName.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namenull", superName.variable().getValue());

    assertEquals("getName", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void getName_non_null_return() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 46);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("getName");
    setName.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("getName", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void errorSomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 75);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("errorSomething", String.class);
    final InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class,
        () -> setName.invoke(myTest, (Object) null));
    assertNull(invocationTargetException.getTargetException().getMessage());

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("errorSomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void throwSomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 87);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("throwSomething", String.class);
    final InvocationTargetException invocationTargetException = assertThrows(InvocationTargetException.class,
        () -> setName.invoke(myTest, (Object) null));
    assertNull(invocationTargetException.getTargetException().getMessage());

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("throwSomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void catchSomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 112);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("catchSomething", String.class);
    setName.invoke(myTest, (Object) null);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("catchSomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void finallySomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 146);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method setName = aClass.getDeclaredMethod("finallySomething", String.class);
    setName.invoke(myTest, (Object) null);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("finallySomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void conditionalThrow() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 163);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("conditionalThrow", int.class, int.class);
    method.invoke(myTest, 1, 2);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.never())
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    assertThrows(InvocationTargetException.class, () -> method.invoke(myTest, 3, 2));
    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("conditionalThrow", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void breakSomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 174);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("breakSomething");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("breakSomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void continueSomething() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 187);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("continueSomething");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("some name", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namesome name", superName.variable().getValue());

    assertEquals("continueSomething", snapshot.getFrames(0).getMethodName());
  }


  @Test
  void superConstructor() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPSuperClass.java", 10);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String superName = "com/intergral/deep/test/target/BPSuperClass";
    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(superName);
    classLoader.setBytes(name, ByteClassLoader.loadBytes(name));

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(superName);
    final byte[] transformed = instrumentationService.transform(null, superName, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        superName + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    // modify the super class with the tracepoint
    final String clazzName = InstUtils.externalClassName(superName);
    classLoader.setBytes(clazzName, transformed);
    classLoader.setBytes(InstUtils.externalClassName(name), ByteClassLoader.loadBytes(name));

    // now load the subclass
    final Class<?> superClass = classLoader.loadClass(clazzName);
    final Class<?> aClass = classLoader.loadClass(InstUtils.externalClassName(name));
    assertNotNull(aClass);
    assertEquals(aClass.getSuperclass(), superClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance("some name", 4);
    assertNotNull(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertTrue(scanResult.found());
    assertEquals("i am a namesome name", scanResult.variable().getValue());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be null
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    assertEquals("<init>", snapshot.getFrames(0).getMethodName());
  }

  @Test
  void multipleTps_oneLine() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 44);
    final MockTracepointConfig tracepointConfig2 = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 44);
    tracepointRef.set(Arrays.asList(tracepointConfig, tracepointConfig2));

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
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("getName");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(2))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namenull", superName.variable().getValue());

    assertEquals("getName", snapshot.getFrames(0).getMethodName());
  }

  @Test
  void multipleTps_nextLine() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 42);
    final MockTracepointConfig tracepointConfig2 = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 44);
    tracepointRef.set(Arrays.asList(tracepointConfig, tracepointConfig2));

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
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("getName");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(2))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namenull", superName.variable().getValue());

    assertEquals("getName", snapshot.getFrames(0).getMethodName());
  }

  @Test
  void someFunctionWithABody() throws Exception {

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 151);
    tracepointRef.set(Collections.singletonList(tracepointConfig));

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
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("someFunctionWithABody", String.class);
    method.invoke(myTest, "some string");

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);
    final IVariableScan scanResult = SnapshotUtils.findVarByName("name", snapshot);

    // there should not be a local var called name
    assertFalse(scanResult.found());

    // we should also find the 'this' object which should be the BPTargetClass
    final IVariableScan thisScan = SnapshotUtils.findVarByName("this", snapshot);
    assertTrue(thisScan.found());
    assertEquals(myTest.getClass().getName(), thisScan.variable().getType());

    // on the 'this' object we should find the field 'name' which should be the value we set in the constructor
    final Variable variable = thisScan.variable();
    final IVariableScan thisName = SnapshotUtils.findVarByName("name", variable.getChildrenList(), snapshot.getVarLookupMap());
    assertTrue(thisName.found());
    assertEquals("null", thisName.variable().getValue());

    // there should however be a value for the 'super.name' field that is set to the known value
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(superName.found());
    assertEquals("i am a namenull", superName.variable().getValue());

    assertEquals("someFunctionWithABody", snapshot.getFrames(0).getMethodName());
  }

  // cannot seem to load cf class files
  // consistent issues with verifier
//  @Test
//  void cfVisitor() throws Exception {
//    final MockTracepointConfig tracepointConfig = new MockTracepointConfig("/src/main/cfml/testFile.cfm", 3);
//    tracepointRef.set(Collections.singletonList(tracepointConfig));
//    // we need to process the cfm tracepoints
//    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));
//
//    final String name = "cftestFile2ecfm137384933";
//    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(name);
//    final byte[] originalBytes = byteClassLoader.getBytes(name);
//
//    // for adobe cf we need a location url to be set
//    cfUrl.set(new URL("file:///src/main/cfml/testFile.cfm"));
//
//    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
//    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
//    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed, name + Thread.currentThread().getStackTrace()[1].getMethodName());
//
//    assertNotNull(transformed, "Failed to transform the test class!");
//    assertNotEquals(originalBytes.length, transformed.length);
//
//    byteClassLoader.setBytes("coldfusion.runtime.CfJspPage", ByteClassLoader.loadBytes("coldfusion/runtime/CfJspPage"));
//    byteClassLoader.setBytes("coldfusion.runtime.CFPage", ByteClassLoader.loadBytes("coldfusion/runtime/CFPage"));
//    byteClassLoader.setBytes(name, transformed);
//    byteClassLoader.loadClass("java.lang.Object");
//    final Class<?> jspPageClass = byteClassLoader.loadClass("coldfusion.runtime.CfJspPage");
//    assertNotNull(jspPageClass);
//    final Class<?> pageClass = byteClassLoader.loadClass("coldfusion.runtime.CFPage");
//    assertNotNull(pageClass);
//    final Class<?> aClass = byteClassLoader.loadClass(name);
//
//    final Constructor<?> constructor = aClass.getConstructor();
//    final Object instance = constructor.newInstance();
//    final Method runPage = aClass.getDeclaredMethod("runPage");
//    runPage.setAccessible(true);
//    runPage.invoke(instance);
//
//    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);
//
//    Mockito.verify(pushService, Mockito.times(1))
//        .pushSnapshot(argumentCaptor.capture(), Mockito.any());
//
//  }


  @Test
  void jspVisitorTest() throws Exception {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig("/src/main/webapp/tests/string.jsp", 9);
    tracepointRef.set(Collections.singletonList(tracepointConfig));
    // we need to process the jsp tracepoints
    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final String name = "org/apache/jsp/tests/string_jsp";
    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(name);
    final byte[] originalBytes = byteClassLoader.getBytes(name);

    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed, name + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String className = InstUtils.externalClassName(name);
    byteClassLoader.setBytes(className, transformed);
    final Class<?> aClass = byteClassLoader.loadClass(className);

    final JspFactory jspFactory = Mockito.mock(JspFactory.class);
    final PageContext pageContext = Mockito.mock(PageContext.class);
    final JspWriter jspWriter = Mockito.mock(JspWriter.class);
    Mockito.when(pageContext.getOut()).thenReturn(jspWriter);

    JspFactory.setDefaultFactory(jspFactory);
    Mockito.when(jspFactory.getPageContext(Mockito.any(Servlet.class), Mockito.any(ServletRequest.class), Mockito.any(
            ServletResponse.class), Mockito.eq(null), Mockito.anyBoolean(), Mockito.anyInt(), Mockito.anyBoolean()))
        .thenReturn(pageContext);

    final Constructor<?> constructor = aClass.getConstructor();
    final Object instance = constructor.newInstance();

    final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
    Mockito.when(servletRequest.getDispatcherType()).thenReturn(DispatcherType.ERROR);
    final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);

    final Method jspService = aClass.getDeclaredMethod("_jspService", HttpServletRequest.class, HttpServletResponse.class);
    jspService.invoke(instance, servletRequest, servletResponse);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, Mockito.times(1))
        .pushSnapshot(argumentCaptor.capture(), Mockito.any());

    final EventSnapshot value = argumentCaptor.getValue();

    final Snapshot snapshot = PushUtils.convertToGrpc(value);

    assertEquals("_jspService", snapshot.getFrames(0).getMethodName());
    assertEquals("org.apache.jsp.tests.string_jsp", snapshot.getFrames(0).getClassName());
    assertEquals("string.jsp", snapshot.getFrames(0).getFileName());
    assertEquals(9, snapshot.getFrames(0).getLineNumber());
    assertEquals("string_jsp.java", snapshot.getFrames(0).getTranspiledFileName());
    assertEquals(127, snapshot.getFrames(0).getTranspiledLineNumber());
  }
}