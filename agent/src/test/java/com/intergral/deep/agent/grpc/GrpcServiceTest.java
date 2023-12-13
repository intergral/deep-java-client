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

package com.intergral.deep.agent.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.intergral.deep.agent.api.auth.IAuthProvider;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.logging.Logger;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.proto.poll.v1.PollRequest;
import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotResponse;
import com.intergral.deep.tests.grpc.TestInterceptor;
import com.intergral.deep.tests.grpc.TestPollService;
import com.intergral.deep.tests.grpc.TestSnapshotService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcServiceTest {

  private GrpcService grpcService;
  private Server server;
  private CountDownLatch pollLatch;
  private final AtomicReference<PollRequest> pollRequest = new AtomicReference<>();
  private CountDownLatch snapshotLatch;

  private final AtomicReference<String> header = new AtomicReference<>();
  private int port;

  @BeforeAll
  static void beforeAll() {
    System.setProperty("java.util.logging.config.file", ClassLoader.getSystemResource("logging.properties").getPath());
    Logger.configureLogging(Settings.build(Collections.singletonMap("logging.level", "DEBUG")));
  }

  @BeforeEach
  void setUp() throws Exception {

    final TestInterceptor testInterceptor = new TestInterceptor("test_key");

    pollLatch = new CountDownLatch(1);
    snapshotLatch = new CountDownLatch(1);
    final TestSnapshotService testSnapshotService = new TestSnapshotService((snapshot, responseObserver) -> {
      final String headerVal = testInterceptor.contextKey().get();
      header.set(headerVal);
      snapshotLatch.countDown();
      responseObserver.onNext(SnapshotResponse.newBuilder().build());
      responseObserver.onCompleted();
    });

    final TestPollService testPollService = new TestPollService((request, responseObserver) -> {
      final String headerVal = testInterceptor.contextKey().get();
      header.set(headerVal);
      pollRequest.set(request);
      pollLatch.countDown();
      responseObserver.onNext(PollResponse.newBuilder().setTsNanos(202020L).build());
      responseObserver.onCompleted();
    });

    // find a free port
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }

    server = ServerBuilder.forPort(port)
        .addService(ServerInterceptors.intercept(testPollService.bindService(), testInterceptor))
        .addService(ServerInterceptors.intercept(testSnapshotService.bindService(), testInterceptor)).build();

    server.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    this.grpcService.shutdown();

    this.server.shutdownNow();
    this.server.awaitTermination();
  }

  @Test
  void serverCanConnect_poll() throws InterruptedException {

    final HashMap<String, String> map = new HashMap<>();
    map.put(ISettings.KEY_SERVICE_URL, "localhost:" + port);
    map.put(ISettings.KEY_SERVICE_SECURE, "false");
    map.put(ISettings.KEY_AUTH_PROVIDER, MockAuthProvider.class.getName());

    final Settings build = Settings.build(map);
    build.setPlugins(Collections.singleton(new MockAuthProvider()));
    grpcService = new GrpcService(build);

    final PollResponse pollResponse = grpcService.pollService().poll(PollRequest.newBuilder().setTsNanos(101010L).build());
    assertEquals(202020L, pollResponse.getTsNanos());
    //noinspection ResultOfMethodCallIgnored
    pollLatch.await(5, TimeUnit.SECONDS);

    final PollRequest pollRequest = this.pollRequest.get();
    assertNotNull(pollRequest);

    assertEquals("test_header", header.get());
  }

  @Test
  void serverCanConnect_snapshot() throws InterruptedException {

    final HashMap<String, String> map = new HashMap<>();
    map.put(ISettings.KEY_SERVICE_URL, "localhost:" + port);
    map.put(ISettings.KEY_SERVICE_SECURE, "false");
    map.put(ISettings.KEY_AUTH_PROVIDER, MockAuthProvider.class.getName());

    final Settings build = Settings.build(map);
    build.setPlugins(Collections.singleton(new MockAuthProvider()));
    grpcService = new GrpcService(build);

    final CountDownLatch responseLatch = new CountDownLatch(1);
    final AtomicReference<SnapshotResponse> responseAtomicReference = new AtomicReference<>();
    grpcService.snapshotService().send(Snapshot.newBuilder().build(),
        new StreamObserver<SnapshotResponse>() {
          @Override
          public void onNext(final SnapshotResponse value) {
            responseAtomicReference.set(value);
            responseLatch.countDown();
          }

          @Override
          public void onError(final Throwable t) {
            t.printStackTrace();
            fail("Cannot connect snapshot service");
          }

          @Override
          public void onCompleted() {

          }
        });
    //noinspection ResultOfMethodCallIgnored
    responseLatch.await(5, TimeUnit.SECONDS);
    final SnapshotResponse snapshotResponse = responseAtomicReference.get();
    assertNotNull(snapshotResponse);
    assertEquals("test_header", header.get());
  }


  public static class MockAuthProvider implements IAuthProvider, IDeepPlugin {

    @Override
    public Map<String, String> provide() {
      return Collections.singletonMap("test_key", "test_header");
    }
  }
}