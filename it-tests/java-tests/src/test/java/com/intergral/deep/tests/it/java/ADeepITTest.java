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

package com.intergral.deep.tests.it.java;

import com.intergral.deep.proto.common.v1.AnyValue;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.poll.v1.PollRequest;
import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.poll.v1.ResponseType;
import com.intergral.deep.proto.resource.v1.Resource;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotResponse;
import com.intergral.deep.proto.tracepoint.v1.StackFrame;
import com.intergral.deep.proto.tracepoint.v1.TracePointConfig;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.proto.tracepoint.v1.VariableID;
import com.intergral.deep.proto.tracepoint.v1.WatchResult;
import com.intergral.deep.tests.ResettableCountDownLatch;
import com.intergral.deep.tests.grpc.TestPollService;
import com.intergral.deep.tests.grpc.TestSnapshotService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class ADeepITTest {

  protected static ResettableCountDownLatch grpcConnectLatch;
  protected static Server server;
  protected static ResettableCountDownLatch snapshotLatch;
  protected static PollResponse nextResponse;
  protected static Object nerdVision;

  protected static AtomicReference<Snapshot> snapshotAtomicReference = new AtomicReference<>();


  @BeforeAll
  static void beforeAll() throws Exception {
    snapshotLatch = new ResettableCountDownLatch(1);
    grpcConnectLatch = new ResettableCountDownLatch(1);

    if (nerdVision != null) {
      return;
    }
    // we have to be careful with the class loading when testing the agent in our own tests
    // we build all the message objects here to ensure we load into the correct classloader
    //noinspection unused
    final PollResponse build = PollResponse.newBuilder()
        .setCurrentHash("")
        .setResponseType(ResponseType.UPDATE)
        .addResponse(TracePointConfig.newBuilder().setPath("").setLineNumber(1).build())
        .build();
    //noinspection unused
    final PollRequest pollRequest = PollRequest.newBuilder()
        .setResource(Resource.newBuilder().addAttributes(KeyValue.newBuilder()
            .setKey("").setValue(AnyValue.newBuilder().build()).build()).build())
        .build();
    SnapshotResponse.newBuilder().build();
    WatchResult.newBuilder().build();
    //noinspection unused
    final WatchResult.ResultCase goodResult = WatchResult.ResultCase.GOOD_RESULT;
    //noinspection unused
    final Snapshot snapshot = Snapshot.newBuilder()
        .addFrames(StackFrame.newBuilder().addVariables(VariableID.newBuilder().build()).build())
        .putVarLookup("", Variable.newBuilder().build())
        .build();
    // See comment above

    TestPollService pollService = new TestPollService((request, observer) -> {
      if (nextResponse != null) {
        observer.onNext(nextResponse);
      }
      observer.onCompleted();
      grpcConnectLatch.countDown();
    });

    TestSnapshotService snapshotService = new TestSnapshotService((request, responseObserver) -> {
      System.out.println("send");
      snapshotAtomicReference.set(request);
      responseObserver.onCompleted();
      snapshotLatch.countDown();
    });

    server = ServerBuilder.forPort(9898)
        .addService(pollService.bindService())
        .addService(snapshotService.bindService())
        .build();
    server.start();

    //noinspection DataFlowIssue
    final Path rootPath = Paths.get(ADeepITTest.class.getResource("/").toURI())
        .getParent() // test
        .getParent() // src
        .getParent() // java-tests
        .getParent(); // it-tests
    final Path jarPath = rootPath // root
        .resolve("agent/target/agent-1.0-SNAPSHOT.jar");

    final String nvPath = System.getProperty("mvn.agentPath", jarPath.toAbsolutePath().toString());

    final Map<String, String> config = new HashMap<>();
    config.put("service.url", "localhost:9898");
    config.put("service.secure", "false");
    config.put("logging.level", "FINE");
    config.put("deep.path", nvPath);
    config.put("transform.path", rootPath.resolve("dispath").toAbsolutePath().toString());

    ByteBuddyAgent.attach(new File(nvPath), getPid(), configAsArgs(config));

    final Class<?> aClass = Class.forName("com.intergral.deep.agent.AgentImpl");
    final Method registerBreakpointService = aClass.getDeclaredMethod("loadDeepAPI");

    nerdVision = registerBreakpointService.invoke(null);
  }

  static String getPid() {
    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
    return nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
  }

  static String configAsArgs(final Map<String, String> config) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (Map.Entry<String, String> entry : config.entrySet()) {
      if (stringBuilder.length() != 0) {
        stringBuilder.append(',');
      }
      stringBuilder.append(entry.getKey()).append('=').append(entry.getValue());
    }
    return stringBuilder.toString();
  }

  @BeforeEach
  void setUp() {
    snapshotLatch = new ResettableCountDownLatch(1);
    grpcConnectLatch = new ResettableCountDownLatch(1);

  }

  protected void onNext(final PollResponse response) {
    nextResponse = response;
  }
}
