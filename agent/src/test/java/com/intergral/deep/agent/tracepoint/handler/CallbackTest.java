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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intergral.deep.agent.api.plugin.ITraceProvider;
import com.intergral.deep.agent.api.plugin.ITraceProvider.ISpan;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import java.io.Closeable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class CallbackTest {

  private final Settings settings = Mockito.mock(Settings.class);
  private final TracepointConfigService tracepointConfigService = Mockito.mock(TracepointConfigService.class);
  private final PushService pushService = Mockito.mock(PushService.class);
  private final String collect = "";

  @BeforeEach
  void setUp() {
    Callback.init(settings, tracepointConfigService, pushService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", ""})
  @NullSource()
  void spanMustAlwaysReturn(final String name) {
    final Closeable test = Callback.span(name, collect);
    assertNotNull(test);
    assertDoesNotThrow(test::close);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", ""})
  @NullSource()
  void spanMustAlwaysReturn2(final String name) {
    final ITraceProvider iTraceProvider = Mockito.mock(ITraceProvider.class);
    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(iTraceProvider);
    final Closeable test = Callback.span(name, collect);
    assertNotNull(test);
    assertDoesNotThrow(test::close);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", ""})
  @NullSource()
  void spanMustAlwaysReturn3(final String name) {
    final ITraceProvider iTraceProvider = Mockito.mock(ITraceProvider.class);
    final ISpan iSpan = Mockito.mock(ISpan.class);
    Mockito.when(iTraceProvider.createSpan(name)).thenReturn(iSpan);
    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(iTraceProvider);
    final Closeable test = Callback.span(name, collect);
    assertNotNull(test);
    assertDoesNotThrow(test::close);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", ""})
  @NullSource()
  void spanMustAlwaysReturn4(final String name) throws Exception {
    final ITraceProvider iTraceProvider = Mockito.mock(ITraceProvider.class);
    final ISpan iSpan = Mockito.mock(ISpan.class);
    Mockito.doThrow(new RuntimeException("test")).when(iSpan).close();
    Mockito.when(iTraceProvider.createSpan(name)).thenReturn(iSpan);
    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(iTraceProvider);
    final Closeable test = Callback.span(name, collect);
    assertNotNull(test);
    assertDoesNotThrow(test::close);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", ""})
  @NullSource()
  void spanMustAlwaysReturn5(final String name) {
    final ITraceProvider iTraceProvider = Mockito.mock(ITraceProvider.class);
    Mockito.doThrow(new RuntimeException("test")).when(iTraceProvider).createSpan(name);
    Mockito.when(settings.getPlugin(ITraceProvider.class)).thenReturn(iTraceProvider);
    final Closeable test = Callback.span(name, collect);
    assertNotNull(test);
    assertDoesNotThrow(test::close);
  }
}