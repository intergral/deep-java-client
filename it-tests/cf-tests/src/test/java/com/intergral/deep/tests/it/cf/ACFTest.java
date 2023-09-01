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

package com.intergral.deep.tests.it.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.poll.v1.ResponseType;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.StackFrame;
import com.intergral.deep.proto.tracepoint.v1.TracePointConfig;
import com.intergral.deep.tests.grpc.TestPollService;
import com.intergral.deep.tests.grpc.TestSnapshotService;
import com.intergral.deep.tests.snapshot.SnapshotUtils;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public abstract class ACFTest {

  private static final AtomicReference<Snapshot> snapshot = new AtomicReference<>();
  private static CountDownLatch loglatch;
  private static Server server;
  private static CountDownLatch snapshotlatch;
  private final GenericContainer<?> container;
  private TestPollService pollService;


  public ACFTest(final String dockerImageName) {
    loglatch = new CountDownLatch(1);
    snapshotlatch = new CountDownLatch(1);

    Path agentTarget = null;
    Path testFilePath = null;
    Path jvmConfigPath = null;
    try {
      final Path targetPath = Paths.get(ACFTest.class.getResource("/").toURI()).getParent();
      agentTarget = targetPath.getParent()
          .getParent()
          .getParent()
          .resolve("agent/target/agent-1.0-SNAPSHOT.jar");
      testFilePath = targetPath.getParent().resolve("src/test/cfml/testFile.cfm");
      jvmConfigPath = targetPath.getParent().resolve("src/test/resources/jvm.config");
    } catch (URISyntaxException e) {
      fail(e.getMessage());
    }

    final String property = System.getProperty("mvn.agentPath", agentTarget.toString());

    final String testHost = System.getProperty("nv.test.host", "172.17.0.1");

    container = new GenericContainer<>(new ImageFromDockerfile("cftest", true)
        // add the nv jar into the context of the docker file
        .withFileFromPath("deep.jar", new File(property).toPath())
        .withFileFromPath("testFile.cfm", testFilePath.toFile().toPath())
        .withFileFromPath("jvm.config", jvmConfigPath.toFile().toPath())
        // extend the CF 11 docker
        .withDockerfileFromBuilder(
            builder -> builder.from(dockerImageName)
                // add the new build NV into the docker
                .copy("deep.jar", "/opt/deep/deep.jar")
                // add the cfm test file to the docker
                .copy("testFile.cfm", "/app/CTA/tests/testFile.cfm")
                // override jvm config for cf
                .copy("jvm.config", "/opt/coldfusion/cfusion/bin/jvm.config")
        ))
        // consume logs to scan for cf start up
        .withLogConsumer(outputFrame -> {
          System.out.println(outputFrame.getUtf8String());
          if (outputFrame.getUtf8String().contains("ColdFusion started")) {
            loglatch.countDown();
          }
        })
        .withExposedPorts(8500)

        // required envs for adobe image
        .withEnv("acceptEULA", "YES")
        .withEnv("password", "admin")

        // set Deep options

        .withEnv("DEEP_SERVICE_URL", testHost + ":9999")
        .withEnv("DEEP_SERVICE_SECURE", "false")
        .withEnv("DEEP_LOGGING_LEVEL", "FINE");

  }


  @BeforeEach
  void setUp() throws Exception {
    final CountDownLatch grpcConnectLatch = new CountDownLatch(1);

    pollService = new TestPollService((request, responseObserver) -> {
      responseObserver.onNext(PollResponse.newBuilder()
          .setResponseType(ResponseType.UPDATE)
          .setCurrentHash("1")
          .addResponse(TracePointConfig.newBuilder()
              .setPath("/src/main/cfml/tests/testFile.cfm")
              .setLineNumber(3)
              .build())
          .build());
      responseObserver.onCompleted();
      grpcConnectLatch.countDown();
    });

    final TestSnapshotService testSnapshotService = new TestSnapshotService(
        (request, responseObserver) -> {
          snapshot.set(request);
          snapshotlatch.countDown();
        });

    server = ServerBuilder.forPort(9999)
        .addService(pollService.bindService())
        .addService(testSnapshotService.bindService())
        .build();
    server.start();
    container.start();

    // await the initial deep grpc connection
    assertTrue(grpcConnectLatch.await(600, TimeUnit.SECONDS));
    System.out.println("GRPC Connected");

    // await cf start up
    assertTrue(loglatch.await(600, TimeUnit.SECONDS));
    System.out.println("CF Started.");
  }


  @AfterEach
  void afterAll() {
    server.shutdownNow();
    container.stop();
  }


  @Test
  void checkCfTracepoint() throws Exception {
    final String uri =
        "http://" + container.getContainerIpAddress() + ":" + container.getMappedPort(8500)
            + "/CTA/tests/testFile.cfm";
    final Request build = new Request.Builder().url(uri).build();

    final OkHttpClient client = new OkHttpClient.Builder().build();

    // cf can take a while to be ready even after it says it is started
    // so we loop the request
    while (true) {
      final Call call = client.newCall(build);
      try (final Response execute = call.execute()) {
        final ResponseBody body = execute.body();
        final String s = new String(body.bytes());
        System.out.println(s);
        break;
      } catch (Exception e) {
        e.printStackTrace();
      }
      Thread.sleep(1000);
    }

    assertTrue(snapshotlatch.await(5, TimeUnit.MINUTES));

    final Snapshot snapshot = ACFTest.snapshot.get();

    assertNotNull(snapshot);

    final StackFrame topFrame = snapshot.getFrames(0);
    assertEquals("app/CTA/tests/testFile.cfm", topFrame.getFileName());
    assertEquals("runPage", topFrame.getMethodName());
    assertTrue(topFrame.getAppFrame());

    final SnapshotUtils.IVariableScan result = SnapshotUtils.findVarByName("VARIABLES",
        topFrame.getVariablesList(),
        snapshot.getVarLookupMap());
    assertTrue(result.found());
    assertEquals("VARIABLES", result.variableId().getName());
    assertEquals("coldfusion.runtime.VariableScope", result.variable().getType());

    final SnapshotUtils.IVariableScan iResult = SnapshotUtils.findVarByName("I",
        result.variable().getChildrenList(),
        snapshot.getVarLookupMap());
    assertTrue(iResult.found());
    assertEquals("I", iResult.variableId().getName());
    assertEquals("java.lang.Integer", iResult.variable().getType());
    assertEquals("100", iResult.variable().getValue());
  }

}
