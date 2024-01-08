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
import static org.mockito.Mockito.times;

import com.intergral.deep.agent.api.plugin.ITraceProvider;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
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
import com.intergral.deep.agent.types.snapshot.StackFrame;
import com.intergral.deep.agent.types.snapshot.WatchResult;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import lucee.runtime.PageSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * We test the visitor by using the transformer to modify classes.
 * <p>
 * This test can be used to play with visitor, it uses the {@link BPTestTarget} class to install tracepoints into
 * <p>
 * To run this use the 'VisitorTest' saved config for idea, or add
 * {@code -Ddeep.callback.class=com.intergral.deep.agent.tracepoint.handler.Callback} to the test runner args
 * <p>
 * WARNING: The line numbers used in this test are important, they must match the line numbers on the test target classes!!
 */
class VisitorTest {

  private final Settings settings = Mockito.mock(Settings.class);
  private final PushService pushService = Mockito.mock(PushService.class);
  private final Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
  private final String disPath = Paths.get(Paths.get(".").normalize().toAbsolutePath().getParent().toString(), "dispath").toString();

  private final AtomicReference<Collection<TracePointConfig>> tracepointRef = new AtomicReference<>();
  private final AtomicReference<URL> cfUrl = new AtomicReference<>();
  private TracepointConfigService tracepointConfigService;
  private TracepointInstrumentationService instrumentationService;

  @BeforeAll
  static void beforeAll() {
    final Settings settings = Mockito.mock(Settings.class);
    // set up logging to help with debugging tests
    Mockito.when(settings.getSettingAs("logging.level", Level.class)).thenReturn(Level.FINEST);
    Logger.configureLogging(settings);
  }

  @BeforeEach
  void setUp() {
    Mockito.when(settings.getResource()).thenReturn(Resource.DEFAULT);

    // for these tests we do not care about these (they are tested in the TracepointInstrumentationServiceTest)
    // we just need them to pass, as we manage the instrumentation ourselves.
    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[0]);
    Mockito.when(instrumentation.isModifiableClass(Mockito.any())).thenReturn(true);

    // these settings are needed for jsp transforms
    Mockito.when(settings.getAsList("jsp.packages")).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.packages", List.class)).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.suffix", String.class)).thenReturn("_jsp");

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
    tracepointConfigService = new TracepointConfigService(instrumentationService) {
      @Override
      public Collection<TracePointConfig> loadTracepointConfigs(final Collection<String> tracepointId) {
        this.installedTracepoints.clear();
        this.installedTracepoints.addAll(tracepointRef.get());
        return super.loadTracepointConfigs(tracepointId);
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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    final IVariableScan superName = SnapshotUtils.findVarByName("BPSuperClass.name", variable.getChildrenList(),
        snapshot.getVarLookupMap());
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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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
        .pushSnapshot(argumentCaptor.capture());

    assertThrows(InvocationTargetException.class, () -> method.invoke(myTest, 3, 2));
    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(2))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(2))
        .pushSnapshot(argumentCaptor.capture());

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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

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


  @Test
  void cfVisitor() throws Exception {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig("/src/main/cfml/testFile.cfm", 3);
    tracepointRef.set(Collections.singletonList(tracepointConfig));
    // we need to process the cfm tracepoints
    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final String name = "cftestFile2ecfm137384933";
    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(name);
    final byte[] originalBytes = byteClassLoader.getBytes(name);

    final JspFactory jspFactory = Mockito.mock(JspFactory.class);
    final PageContext pageContext = Mockito.mock(PageContext.class);
    final JspWriter jspWriter = Mockito.mock(JspWriter.class);
    Mockito.when(pageContext.getOut()).thenReturn(jspWriter);

    JspFactory.setDefaultFactory(jspFactory);
    Mockito.when(jspFactory.getPageContext(Mockito.any(Servlet.class), Mockito.any(ServletRequest.class), Mockito.any(
            ServletResponse.class), Mockito.eq(null), Mockito.anyBoolean(), Mockito.anyInt(), Mockito.anyBoolean()))
        .thenReturn(pageContext);

    // for adobe cf we need a location url to be set
    cfUrl.set(new URL("file:///src/main/cfml/testFile.cfm"));

    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    // we cannot load the cf class in test case for some reason
    // consistent issues with verifier
    //    byteClassLoader.setBytes(name, transformed);
    //    final List<String> classes = Arrays.asList("java.lang.Object", "coldfusion.tagext.io.OutputTag",
    //    "coldfusion.runtime.NeoPageContext",
    //        "coldfusion.runtime.CfJspPage", "coldfusion.runtime.CFPage");
    //    for (String s : classes) {
    //      final Class<?> aClass = byteClassLoader.loadClass(s);
    //      assertNotNull(aClass);
    //      assertEquals(aClass.getName(), s);
    //    }
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

  }


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
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    final EventSnapshot value = argumentCaptor.getValue();

    final Snapshot snapshot = PushUtils.convertToGrpc(value);

    assertEquals("_jspService", snapshot.getFrames(0).getMethodName());
    assertEquals("org.apache.jsp.tests.string_jsp", snapshot.getFrames(0).getClassName());
    assertEquals("string.jsp", snapshot.getFrames(0).getFileName());
    assertEquals(9, snapshot.getFrames(0).getLineNumber());
    assertEquals("string_jsp.java", snapshot.getFrames(0).getTranspiledFileName());
    assertEquals(127, snapshot.getFrames(0).getTranspiledLineNumber());
  }

  @Test
  void luceeVisitorTest() throws Exception {
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig("/tests/testFile.cfm", 3);
    tracepointRef.set(Collections.singletonList(tracepointConfig));
    // we need to process the jsp tracepoints
    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    cfUrl.set(new URL("file:///tests/testFile.cfm"));
    final String name = "testfile_cfm$cf";
    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(name);
    final byte[] originalBytes = byteClassLoader.getBytes(name);

    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform test class.");
    assertNotEquals(originalBytes.length, transformed.length);

    final String className = InstUtils.externalClassName(name);
    byteClassLoader.setBytes(className, transformed);
    final Class<?> aClass = byteClassLoader.loadClass(className);

    final Constructor<?> constructor = aClass.getConstructor(PageSource.class);
    final Object instance = constructor.newInstance(new PageSource());
    final Method call = aClass.getMethod("call", lucee.runtime.PageContext.class);
    call.invoke(instance, new lucee.runtime.PageContext());

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    final EventSnapshot value = argumentCaptor.getValue();
    assertNotNull(value);
    // Cannot really verify variables as we are using fake classes to run this test

    final StackFrame stackFrame = value.getFrames().iterator().next();
    assertEquals("testFile.cfm", stackFrame.getFileName());
    assertEquals(3, stackFrame.getLineNumber());
    assertEquals("testfile_cfm$cf", stackFrame.getClassName());
  }

  @Test
  void methodWrapperTest() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);
    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 151)
        .withArg(TracePointConfig.METHOD_NAME, "someFunctionWithABody").withArg(TracePointConfig.SPAN, TracePointConfig.METHOD);

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);

    Mockito.verify(traceProviderSpy, times(1)).createSpan("someFunctionWithABody(Ljava/lang/String;)Ljava/lang/String;");

    final IVariableScan someArg = SnapshotUtils.findVarByName("someArg", snapshot);

    assertTrue(someArg.found());

    assertEquals("some string", someArg.variable().getValue());
  }

  @Test
  void methodWrapperVoidTest() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 51)
        .withArg(TracePointConfig.METHOD_NAME, "setName").withArg(TracePointConfig.SPAN, TracePointConfig.METHOD);

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("setName", String.class);
    method.invoke(myTest, "some string");

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);

    Mockito.verify(traceProviderSpy, times(1)).createSpan("setName(Ljava/lang/String;)V");

    final IVariableScan someArg = SnapshotUtils.findVarByName("name", snapshot);

    assertTrue(someArg.found());

    assertEquals("some string", someArg.variable().getValue());

  }

  @Test
  void methodWrapperVoidTestWithTP() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", 51)
        .withArg(TracePointConfig.SPAN, TracePointConfig.METHOD);

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("setName", String.class);
    method.invoke(myTest, "some string");

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    final EventSnapshot value = argumentCaptor.getValue();
    assertEquals(tracepointConfig.getId(), value.getTracepoint().getId());

    final Snapshot snapshot = PushUtils.convertToGrpc(value);

    Mockito.verify(traceProviderSpy, times(1)).createSpan("setName(Ljava/lang/String;)V");

    final IVariableScan someArg = SnapshotUtils.findVarByName("name", snapshot);

    assertTrue(someArg.found());

    assertEquals("some string", someArg.variable().getValue());

  }

  @Test
  void methodEntryTP() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", -1)
        .withArg(TracePointConfig.METHOD_NAME, "someOverloaded")
        .withArg(TracePointConfig.SPAN, TracePointConfig.METHOD)
        .withArg(TracePointConfig.FIRE_COUNT, "-1")
        .withArg(TracePointConfig.FIRE_PERIOD, "0");

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("someOverloaded");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(2))
        .pushSnapshot(argumentCaptor.capture());

    Mockito.verify(traceProviderSpy, times(1)).createSpan("someOverloaded()V");
    Mockito.verify(traceProviderSpy, times(1)).createSpan("someOverloaded(Ljava/lang/String;)V");

    final EventSnapshot first = argumentCaptor.getAllValues().get(0);
    assertEquals(tracepointConfig.getId(), first.getTracepoint().getId());
    final Snapshot snapshotTheFirst = PushUtils.convertToGrpc(first);

    final IVariableScan variableScanTheFirst = SnapshotUtils.findVarByName("name", snapshotTheFirst);
    assertFalse(variableScanTheFirst.found());

    final EventSnapshot second = argumentCaptor.getAllValues().get(1);
    assertEquals(tracepointConfig.getId(), second.getTracepoint().getId());
    final Snapshot snapshotTheSecond = PushUtils.convertToGrpc(second);

    final IVariableScan variableScanTheSecond = SnapshotUtils.findVarByName("name", snapshotTheSecond);
    assertTrue(variableScanTheSecond.found());

    assertEquals("test", variableScanTheSecond.variable().getValue());
  }

  @Test
  void methodEntryTPCaptureReturn() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", -1)
        .withArg(TracePointConfig.METHOD_NAME, "getName")
        .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_END)
        .withArg(TracePointConfig.FIRE_COUNT, "-1")
        .withArg(TracePointConfig.FIRE_PERIOD, "0");

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

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

    Mockito.verify(pushService, times(1))
        .pushSnapshot(argumentCaptor.capture());

    Mockito.verify(traceProviderSpy, times(0)).createSpan("getName()Ljava/lang/String");

    watchValidator("return").accept(argumentCaptor.getAllValues());
  }

  @Test
  void methodEntryTPNoTrace() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", -1)
        .withArg(TracePointConfig.METHOD_NAME, "someOverloaded")
        .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_CAPTURE)
        .withArg(TracePointConfig.FIRE_COUNT, "-1")
        .withArg(TracePointConfig.FIRE_PERIOD, "0");

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("someOverloaded");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(2))
        .pushSnapshot(argumentCaptor.capture());

    Mockito.verify(traceProviderSpy, times(0)).createSpan("someOverloaded()V");
    Mockito.verify(traceProviderSpy, times(0)).createSpan("someOverloaded(Ljava/lang/String;)V");

    final EventSnapshot first = argumentCaptor.getAllValues().get(1);
    assertEquals(tracepointConfig.getId(), first.getTracepoint().getId());
    final Snapshot snapshotTheFirst = PushUtils.convertToGrpc(first);

    final IVariableScan variableScanTheFirst = SnapshotUtils.findVarByName("name", snapshotTheFirst);
    assertFalse(variableScanTheFirst.found());

    final EventSnapshot second = argumentCaptor.getAllValues().get(0);
    assertEquals(tracepointConfig.getId(), second.getTracepoint().getId());
    final Snapshot snapshotTheSecond = PushUtils.convertToGrpc(second);

    final IVariableScan variableScanTheSecond = SnapshotUtils.findVarByName("name", snapshotTheSecond);
    assertTrue(variableScanTheSecond.found());

    assertEquals("test", variableScanTheSecond.variable().getValue());
  }

  @Test
  void methodEntryTPTraceOnly() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", -1)
        .withArg(TracePointConfig.METHOD_NAME, "someOverloaded")
        .withArg(TracePointConfig.SPAN, TracePointConfig.METHOD)
        .withArg(TracePointConfig.SNAPSHOT, TracePointConfig.NO_COLLECT)
        .withArg(TracePointConfig.FIRE_COUNT, "-1")
        .withArg(TracePointConfig.FIRE_PERIOD, "0");

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("someOverloaded");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(0))
        .pushSnapshot(argumentCaptor.capture());

    Mockito.verify(traceProviderSpy, times(1)).createSpan("someOverloaded()V");
    Mockito.verify(traceProviderSpy, times(1)).createSpan("someOverloaded(Ljava/lang/String;)V");
  }

  @Test
  void methodEntryTPTraceOnlyConditional() throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final MockTracepointConfig tracepointConfig = Mockito.spy(new MockTracepointConfig(
        "/agent/src/test/java/com/intergral/deep/test/target/BPTestTarget.java", -1)
        .withArg(TracePointConfig.CONDITION, "name == null")
        .withArg(TracePointConfig.METHOD_NAME, "someOverloaded")
        .withArg(TracePointConfig.SPAN, TracePointConfig.METHOD)
        .withArg(TracePointConfig.SNAPSHOT, TracePointConfig.NO_COLLECT)
        .withArg(TracePointConfig.FIRE_COUNT, "-1")
        .withArg(TracePointConfig.FIRE_PERIOD, "0"));

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance(null, 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("someOverloaded");
    method.invoke(myTest);

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(0))
        .pushSnapshot(argumentCaptor.capture());

    Mockito.verify(traceProviderSpy, times(0)).createSpan("someOverloaded()V");
    Mockito.verify(traceProviderSpy, times(0)).createSpan("someOverloaded(Ljava/lang/String;)V");

    // difficult to verify this test - bet we can do is check the tracepoint was used
    Mockito.verify(tracepointConfig, times(2)).getCondition();
  }

  public static class MockTraceProvider implements IDeepPlugin, ITraceProvider {

    @Override
    public ISpan createSpan(final String name) {
      return new ISpan() {
        @Override
        public String name() {
          return name;
        }

        @Override
        public String traceId() {
          return "-1";
        }

        @Override
        public String spanId() {
          return "-2";
        }

        @Override
        public void addAttribute(final String key, final String value) {

        }

        @Override
        public void close() {

        }
      };
    }

    @Override
    public ISpan currentSpan() {
      return null;
    }
  }


  @ParameterizedTest()
  @MethodSource("tracepointLocations")
  void testTracepointLocations(final String testName, final TracePointConfig config, final int snapshots,
      final Consumer<List<EventSnapshot>> validate)
      throws Exception {
    final MockTraceProvider traceProvider = new MockTraceProvider();
    final MockTraceProvider traceProviderSpy = Mockito.spy(traceProvider);

    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(traceProviderSpy);

    final TracePointConfig tracepointConfig = Mockito.spy(config);

    tracepointRef.set(Collections.singletonList(tracepointConfig));

    final String name = "com/intergral/deep/test/target/BPTestTarget";
    final ByteClassLoader classLoader = ByteClassLoader.forFile(name);

    instrumentationService.processBreakpoints(Collections.singletonList(tracepointConfig));

    final byte[] originalBytes = classLoader.getBytes(name);
    final byte[] transformed = instrumentationService.transform(null, name, null, null, originalBytes);
    // we do this here so each test can save the modified bytes, else as they all use the same target class they would stomp over each other
    TransformerUtils.storeUnsafe(disPath, originalBytes, transformed,
        name + "_" + testName + "_" + Thread.currentThread().getStackTrace()[1].getMethodName());

    assertNotNull(transformed, "Failed to transform the test class!");
    assertNotEquals(originalBytes.length, transformed.length);

    final String clazzName = InstUtils.externalClassName(name);
    classLoader.setBytes(clazzName, transformed);

    final Class<?> aClass = classLoader.loadClass(clazzName);
    assertNotNull(aClass);

    final Constructor<?> constructor = aClass.getConstructor(String.class, int.class);
    final Object myTest = constructor.newInstance("location test", 4);
    assertNotNull(myTest);

    final Method method = aClass.getDeclaredMethod("locationTestTarget");
    try {
      method.invoke(myTest);
    } catch (Exception ignored) {

    }

    final ArgumentCaptor<EventSnapshot> argumentCaptor = ArgumentCaptor.forClass(EventSnapshot.class);

    Mockito.verify(pushService, times(snapshots))
        .pushSnapshot(argumentCaptor.capture());

    validate.accept(argumentCaptor.getAllValues());
  }

  public static Stream<Arguments> tracepointLocations() {
    return Stream.of(
        Arguments.of("StartLine205", new MockTracepointConfig("BPTestTarget.java", 205), 1,
            variableValidator(Collections.singletonMap("this", "BPTestTarget"))),
        Arguments.of("EndLine205",
            new MockTracepointConfig("BPTestTarget.java", 205).withArg(TracePointConfig.STAGE, TracePointConfig.LINE_END), 1,
            variableValidator(Collections.singletonMap("this", "BPTestTarget"))),
        Arguments.of("StartLine207", new MockTracepointConfig("BPTestTarget.java", 207), 1,
            variableValidator(Collections.singletonMap("this.name", "location test"))),
        Arguments.of("EndLine207",
            new MockTracepointConfig("BPTestTarget.java", 207).withArg(TracePointConfig.STAGE, TracePointConfig.LINE_END), 1,
            variableValidator(Collections.singletonMap("this.name", "locationTestTarget"))),
        Arguments.of("LineCapture207",
            new MockTracepointConfig("BPTestTarget.java", 207).withArg(TracePointConfig.STAGE, TracePointConfig.LINE_CAPTURE), 1,
            variableValidator(Collections.singletonMap("this.name", "location test"))),
        Arguments.of("MethodCapture",
            new MockTracepointConfig("BPTestTarget.java", -1).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget")
                .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_CAPTURE), 1,
            variableValidator(Collections.singletonMap("this.name", "location test"))),
        Arguments.of("MethodCapture_thrown",
            new MockTracepointConfig("BPTestTarget.java", -1).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget")
                .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_CAPTURE), 1, watchValidator("thrown")),
        Arguments.of("MethodEntry",
            new MockTracepointConfig("BPTestTarget.java", -1).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget"), 1,
            variableValidator(Collections.singletonMap("this.name", "location test"))),
        Arguments.of("MethodExit",
            new MockTracepointConfig("BPTestTarget.java", -1).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget")
                .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_END), 1,
            variableValidator(Collections.singletonMap("this.name", "locationTestTarget"))),
        Arguments.of("MethodEntry_withLineTP",
            new MockTracepointConfig("BPTestTarget.java", 207).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget"), 1,
            variableValidator(Collections.singletonMap("this.name", "location test"))),
        Arguments.of("MethodExit_withLineTP",
            new MockTracepointConfig("BPTestTarget.java", 207).withArg(TracePointConfig.METHOD_NAME, "locationTestTarget")
                .withArg(TracePointConfig.STAGE, TracePointConfig.METHOD_END), 1,
            variableValidator(Collections.singletonMap("this.name", "locationTestTarget")))
    );
  }

  public static Consumer<List<EventSnapshot>> watchValidator(final String name) {
    return eventSnapshots -> {
      simpleValidate(eventSnapshots);
      final EventSnapshot eventSnapshot = eventSnapshots.get(0);
      final ArrayList<WatchResult> watches = eventSnapshot.getWatches();
      boolean found = false;
      for (WatchResult watch : watches) {
        if (watch.expression().equals(name)) {
          found = true;
          break;
        }
      }
      assertTrue(found);
    };
  }

  public static Consumer<List<EventSnapshot>> variableValidator(final Map<String, String> vars) {
    return eventSnapshots -> {
      simpleValidate(eventSnapshots);
      final EventSnapshot eventSnapshot = eventSnapshots.get(0);
      final Snapshot snapshot = PushUtils.convertToGrpc(eventSnapshot);
      for (Entry<String, String> entry : vars.entrySet()) {
        final IVariableScan variableScan = SnapshotUtils.findVarByPath(entry.getKey(), snapshot);
        assertTrue(variableScan.found());
        assertEquals(entry.getValue(), variableScan.variable().getValue());
      }
    };
  }

  public static void simpleValidate(final List<EventSnapshot> snaps) {
    assertEquals(1, snaps.size());
  }
}