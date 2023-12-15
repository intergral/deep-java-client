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

package com.intergral.deep.agent.poll;

import static com.intergral.deep.agent.types.TracePointConfig.SINGLE_FRAME_TYPE;
import static com.intergral.deep.agent.types.TracePointConfig.STACK;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.api.plugin.MetricDefinition.Label;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.proto.common.v1.AnyValue;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.poll.v1.PollRequest;
import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.poll.v1.ResponseType;
import com.intergral.deep.proto.tracepoint.v1.LabelExpression;
import com.intergral.deep.proto.tracepoint.v1.Metric;
import com.intergral.deep.proto.tracepoint.v1.MetricType;
import com.intergral.deep.proto.tracepoint.v1.TracePointConfig;
import com.intergral.deep.tests.grpc.TestPollService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LongPollServiceTest {

  private Server server;
  private LongPollService longPollService;

  private final AtomicReference<PollRequest> request = new AtomicReference<>(null);
  private PollResponse response;
  private GrpcService grpcService;

  @BeforeEach
  void setUp() throws IOException {
    final TestPollService testPollService = new TestPollService((req, responseObserver) -> {
      request.set(req);

      responseObserver.onNext(response);

      responseObserver.onCompleted();
    });

    // find a free port
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }

    server = ServerBuilder.forPort(port).addService(testPollService.bindService()).build();

    server.start();

    final HashMap<String, String> agentArgs = new HashMap<>();
    agentArgs.put(ISettings.KEY_SERVICE_URL, "localhost:" + port);
    agentArgs.put(ISettings.KEY_SERVICE_SECURE, "false");
    final Settings settings = Settings.build(agentArgs);
    settings.setActive(true);
    settings.setResource(Resource.create(Collections.singletonMap("test", "resource")));
    grpcService = new GrpcService(settings);
    longPollService = new LongPollService(settings, grpcService);
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdownNow();
    server.awaitTermination();

    grpcService.shutdown();
  }

  @Test
  void canPollServer() {
    final TracepointConfigService configService = mock(TracepointConfigService.class);

    longPollService.setTracepointConfig(configService);
    longPollService.run(100);

    verify(configService).noChange(0L);
  }

  @Test
  void callsNoChangeOnResponse() {
    final TracepointConfigService configService = mock(TracepointConfigService.class);
    longPollService.setTracepointConfig(configService);

    response = PollResponse.newBuilder().setResponseType(ResponseType.NO_CHANGE).build();

    longPollService.run(100);

    verify(configService).noChange(0L);
  }

  @Test
  void callsUpdateOnResponse() {
    final TracepointConfigService configService = mock(TracepointConfigService.class);
    longPollService.setTracepointConfig(configService);

    response = PollResponse.newBuilder().setResponseType(ResponseType.UPDATE).build();

    longPollService.run(100);

    verify(configService, never()).noChange(0L);
  }

  @Test
  void propagateHashOnNextCall() {
    final TracepointInstrumentationService instrumentationService = mock(TracepointInstrumentationService.class);

    final TracepointConfigService configService = spy(new TracepointConfigService(instrumentationService));
    doCallRealMethod().when(configService).configUpdate(Mockito.anyLong(), Mockito.eq("123"), Mockito.anyCollection());
    longPollService.setTracepointConfig(configService);

    response = PollResponse.newBuilder().setResponseType(ResponseType.UPDATE).setCurrentHash("123").build();

    longPollService.run(100);

    assertEquals("", request.get().getCurrentHash());
    assertEquals(100, request.get().getTsNanos());

    response = PollResponse.newBuilder().setResponseType(ResponseType.UPDATE).setCurrentHash("321").build();

    longPollService.run(101);

    assertEquals("123", request.get().getCurrentHash());
    assertEquals(101, request.get().getTsNanos());
    verify(instrumentationService, times(2)).processBreakpoints(Mockito.anyCollection());
  }

  @Test
  void canHandleTracepointResponse() {
    final TracepointInstrumentationService instrumentationService = mock(TracepointInstrumentationService.class);

    final TracepointConfigService configService = spy(new TracepointConfigService(instrumentationService));
    doCallRealMethod().when(configService).configUpdate(Mockito.anyLong(), Mockito.eq("123"), Mockito.anyCollection());
    longPollService.setTracepointConfig(configService);

    response = PollResponse.newBuilder().setResponseType(ResponseType.UPDATE).setCurrentHash("123").addResponse(
            TracePointConfig.newBuilder().setPath("/some/file/path.py").setLineNumber(123).setID("tp-1")
                .putAllArgs(Collections.singletonMap("key", "value")).addWatches("i watch").addTargeting(
                    KeyValue.newBuilder().setKey("key")
                        .setValue(AnyValue.newBuilder().setStringValue("some string").build()).build()).build())
        .build();

    longPollService.run(100);

    //noinspection unchecked
    final ArgumentCaptor<Collection<com.intergral.deep.agent.types.TracePointConfig>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(instrumentationService).processBreakpoints(captor.capture());

    final Collection<com.intergral.deep.agent.types.TracePointConfig> value = captor.getValue();

    assertEquals(1, value.size());

    final com.intergral.deep.agent.types.TracePointConfig next = value.iterator().next();

    assertEquals("tp-1", next.getId());
    assertEquals("/some/file/path.py", next.getPath());
    assertEquals(123, next.getLineNo());
    assertArrayEquals(new String[]{"i watch"}, next.getWatches().toArray());
    assertEquals(1, next.getFireCount());
    assertNull(next.getCondition());
    assertEquals(SINGLE_FRAME_TYPE, next.getFrameType());
    assertEquals(STACK, next.getStackType());
  }

  @Test
  void doesSendResourceOnRequest() {
    final TracepointInstrumentationService instrumentationService = mock(TracepointInstrumentationService.class);
    final TracepointConfigService configService = spy(new TracepointConfigService(instrumentationService));
    longPollService.setTracepointConfig(configService);

    longPollService.run(100);

    assertNotNull(request.get().getResource());
    assertEquals("test", request.get().getResource().getAttributes(0).getKey());
    assertEquals("resource", request.get().getResource().getAttributes(0).getValue().getStringValue());
  }

  @ParameterizedTest()
  @MethodSource("canConvertMetricsSource")
  void canConvertMetrics(final Metric input, final MetricDefinition expected) {
    final List<MetricDefinition> name = longPollService.covertMetrics(Collections.singletonList(input));

    assertEquals(expected, name.iterator().next());
  }

  private static Stream<Arguments> canConvertMetricsSource() {
    return Stream.of(
        Arguments.of(Metric.newBuilder().setName("name").build(),
            new MetricDefinition("name", new ArrayList<>(), "COUNTER", "", "deep_agent", "Metric generated from expression: ", "")),
        Arguments.of(Metric.newBuilder().setName("name").setNamespace("custom").build(),
            new MetricDefinition("name", new ArrayList<>(), "COUNTER", "", "custom", "Metric generated from expression: ", "")),
        Arguments.of(Metric.newBuilder().setName("name").setNamespace("custom").setHelp("This is my metric").build(),
            new MetricDefinition("name", new ArrayList<>(), "COUNTER", "", "custom", "This is my metric", "")),
        Arguments.of(Metric.newBuilder().setName("name").setNamespace("custom").setExpression("this.cnt").build(),
            new MetricDefinition("name", new ArrayList<>(), "COUNTER", "this.cnt", "custom", "Metric generated from expression: this.cnt",
                "")),
        Arguments.of(Metric.newBuilder().setUnit("unit").setName("name").setType(MetricType.GAUGE).setExpression("this.cnt").build(),
            new MetricDefinition("name", new ArrayList<>(), "GAUGE", "this.cnt", "deep_agent", "Metric generated from expression: this.cnt",
                "unit")),
        Arguments.of(Metric.newBuilder().addLabelExpressions(
                    LabelExpression.newBuilder()
                        .setKey("key")
                        .setStatic(AnyValue.newBuilder().setStringValue("some string").build())
                        .build())
                .setUnit("unit").setName("name").setType(MetricType.GAUGE).setExpression("this.cnt").build(),
            new MetricDefinition("name", Collections.singletonList(new Label("key", "some string", null)), "GAUGE", "this.cnt",
                "deep_agent", "Metric generated from expression: this.cnt",
                "unit")),
        Arguments.of(Metric.newBuilder().addLabelExpressions(LabelExpression.newBuilder().setKey("key").setExpression("some.thing").build())
                .setUnit("unit").setName("name").setType(MetricType.GAUGE).setExpression("this.cnt").build(),
            new MetricDefinition("name", Collections.singletonList(new Label("key", null, "some.thing")), "GAUGE", "this.cnt", "deep_agent",
                "Metric generated from expression: this.cnt",
                "unit"))
    );
  }
}