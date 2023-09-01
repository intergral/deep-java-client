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

package com.intergral.deep.agent.tracepoint.inst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;

import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.test.MockTracepointConfig;
import com.intergral.deep.test.target.Person;
import com.intergral.deep.tests.inst.ByteClassLoader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TracepointInstrumentationServiceTest {

  private Instrumentation instrumentation;
  private Settings settings;
  private TracepointInstrumentationService tracepointInstrumentationService;

  @BeforeEach
  void setUp() throws MalformedURLException {
    instrumentation = Mockito.mock(Instrumentation.class);
    settings = Mockito.mock(Settings.class);
    Mockito.when(settings.getSettingAs("jsp.packages", List.class)).thenReturn(Arrays.asList("org.apache.jsp", "jsp"));
    Mockito.when(settings.getSettingAs("jsp.suffix", String.class)).thenReturn("_jsp");
    final URL cfurl = new URL("file:///src/main/cfml/testList.cfm");
    tracepointInstrumentationService = new TracepointInstrumentationService(instrumentation, settings) {
      /**
       * To let us test Adobe CF classes we need to load the protection domain, to get the code source url.
       * <p>
       * We do this by intercepting the call to {@link TracepointInstrumentationService#reTransFormCfClasses(Map, Map)}
       * and mocking the {@link CFClassScanner#getLocation(Class)} method.
       *
       * @param newCFMState      the new CFM state
       * @param previousCFMState the previous CFM state
       *
       * @return the modified {@link CFClassScanner}       */
      @Override
      protected CFClassScanner reTransFormCfClasses(final Map<String, TracePointConfig> newCFMState,
          final Map<String, TracePointConfig> previousCFMState) {
        final CFClassScanner iClassScanner = super.reTransFormCfClasses(newCFMState, previousCFMState);
        return new CFClassScanner(iClassScanner.tracePointConfigMap) {
          @Override
          URL getLocation(final Class<?> loadedClass) {
            if (loadedClass.getName().equals("cftestList2ecfm1060358347")) {
              return cfurl;
            }
            return super.getLocation(loadedClass);
          }
        };
      }
    };
  }

  @Test
  void initShouldRegister() {
    final TracepointInstrumentationService init = TracepointInstrumentationService.init(instrumentation, settings);
    assertNotNull(init);
    Mockito.verify(instrumentation).addTransformer(Mockito.any(), Mockito.anyBoolean());
  }

  @Test
  void processTracepointsShouldNotReTransform() throws UnmodifiableClassException {
    // no matched class do not re-transform
    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{});
    tracepointInstrumentationService.processBreakpoints(Collections.singletonList(new MockTracepointConfig()));

    Mockito.verify(instrumentation, Mockito.never()).retransformClasses(Mockito.any());
  }

  @Test
  void matchedTracepointShouldReTranform() throws UnmodifiableClassException {
    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{Person.class});
    Mockito.when(instrumentation.isModifiableClass(Person.class)).thenReturn(true);
    tracepointInstrumentationService.processBreakpoints(
        Collections.singletonList(new MockTracepointConfig("/com/intergral/deep/Person.java")));

    final ArgumentCaptor<Class> argumentCaptor = ArgumentCaptor.forClass(Class.class);

    Mockito.verify(instrumentation).retransformClasses(argumentCaptor.capture());

    final Class value = argumentCaptor.getValue();
    assertEquals(value, Person.class);
  }

  @Test
  void removingTPShouldReTransform() throws UnmodifiableClassException {
    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{Person.class});
    Mockito.when(instrumentation.isModifiableClass(Person.class)).thenReturn(true);
    tracepointInstrumentationService.processBreakpoints(
        Collections.singletonList(new MockTracepointConfig("/com/intergral/deep/Person.java")));

    final ArgumentCaptor<Class> argumentCaptor = ArgumentCaptor.forClass(Class.class);

    Mockito.verify(instrumentation, times(1)).retransformClasses(argumentCaptor.capture());

    final Class value = argumentCaptor.getValue();
    assertEquals(value, Person.class);

    tracepointInstrumentationService.processBreakpoints(Collections.emptyList());

    Mockito.verify(instrumentation, times(2)).retransformClasses(argumentCaptor.capture());

    final Class value2 = argumentCaptor.getValue();
    assertEquals(value2, Person.class);
  }

  @Test
  void cfClassesTriggerTransform() throws IOException, ClassNotFoundException, UnmodifiableClassException {
    final String cfClassName = "cftestList2ecfm1060358347";
    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(cfClassName);
    final Class<?> cfClass = byteClassLoader.loadClass(cfClassName);

    assertNotNull(cfClass);

    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{cfClass});
    Mockito.when(instrumentation.isModifiableClass(cfClass)).thenReturn(true);

    tracepointInstrumentationService.processBreakpoints(Collections.singletonList(new MockTracepointConfig("/src/main/cfml/testList.cfm")));

    Mockito.verify(instrumentation, times(1)).retransformClasses(cfClass);
    tracepointInstrumentationService.processBreakpoints(Collections.emptyList());

    Mockito.verify(instrumentation, times(2)).retransformClasses(cfClass);
  }

  @Test
  void jspClassesTriggerTransform() throws IOException, ClassNotFoundException, UnmodifiableClassException {
    final String jspClassName = "org/apache/jsp/tests/string_jsp";
    final ByteClassLoader byteClassLoader = ByteClassLoader.forFile(jspClassName);
    final Class<?> jspClass = byteClassLoader.loadClass(InstUtils.externalClassName(jspClassName));

    assertNotNull(jspClass);

    Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{jspClass});
    Mockito.when(instrumentation.isModifiableClass(jspClass)).thenReturn(true);

    tracepointInstrumentationService.processBreakpoints(
        Collections.singletonList(new MockTracepointConfig("/src/main/webapp/tests/string.jsp")));

    Mockito.verify(instrumentation, times(1)).retransformClasses(jspClass);
    tracepointInstrumentationService.processBreakpoints(Collections.emptyList());
    Mockito.verify(instrumentation, times(2)).retransformClasses(jspClass);
  }

  @Test
  void getLocation() throws MalformedURLException {
    final ProtectionDomain protectionDomain = Mockito.mock(ProtectionDomain.class);
    final CodeSource codeSource = Mockito.mock(CodeSource.class);
    Mockito.when(protectionDomain.getCodeSource()).thenReturn(codeSource);
    Mockito.when(codeSource.getLocation()).thenReturn(new URL("http://google.com"));
    final URL location = tracepointInstrumentationService.getLocation(protectionDomain);
    assertEquals("http://google.com", location.toString());
  }
}