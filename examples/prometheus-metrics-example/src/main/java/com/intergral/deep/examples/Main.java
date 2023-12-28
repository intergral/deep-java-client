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

package com.intergral.deep.examples;


import com.intergral.deep.Deep;
import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.api.plugin.MetricDefinition.Label;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.api.DeepAPI;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.snapshots.Unit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This example expects the deep agent to be loaded via the javaagent vm option.
 * <p>
 * See RunConfigurations for IDEA:
 * <ul>
 *   <li>Dynamic Load without JavaAgent</li>
 *   <li>Dynamic Load with JavaAgent</li>
 * </ul>
 */
public class Main {

  /**
   * Main entry for example.
   *
   * @param args the startup arguments
   * @throws Throwable if we error
   */
  public static void main(String[] args) throws Throwable {
    // this is only needed in this example as we are using a local built module
    // if using the dependency from maven you do not need to set the path
    //noinspection DataFlowIssue
    Path jarPath = Paths.get(Main.class.getResource("/").toURI())
        .getParent()
        .getParent()
        .getParent()
        .getParent()
        .resolve("agent/target/agent-1.0-SNAPSHOT.jar");

    // Dynamically configure and attach the deep agent
    Deep.config()
        .setJarPath(jarPath.toAbsolutePath().toString())
        .setValue(ISettings.KEY_SERVICE_URL, "localhost:43315")
        .setValue(ISettings.KEY_SERVICE_SECURE, false)
        .start();

    // different ways to get the API instance
    final Deep instance = Deep.getInstance();
    System.out.println(instance.<IDeep>api().getVersion());
    System.out.println(instance.<IReflection>reflection());

    System.out.println(DeepAPI.api().getVersion());
    System.out.println(DeepAPI.reflection());

    Histogram histogram = Histogram.builder()
        .name("request_latency_seconds")
        .help("request latency in seconds")
        .unit(Unit.SECONDS)
        .labelNames("path", "status")
        .register();

    try (HTTPServer server = HTTPServer.builder()
        .port(9400)
        .buildAndStart()) {

      System.out.println("HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");

      final List<Label> labels = new ArrayList<>();
      // USe the API to create a tracepoint that will fire forever
      final Map<String, String> fireCount = new HashMap<>();
      fireCount.put("fire_count", "-1");
      fireCount.put("log_msg", "This is a log message {this}");

      DeepAPI.api()
          .registerTracepoint("com/intergral/deep/examples/SimpleTest", 46,
              fireCount, Collections.emptyList(),
              Collections.singletonList(
                  new MetricDefinition("custom_metric", labels, "HISTOGRAM", "this.cnt", "deep", "help message", "unit")));

      Random random = new Random(0);
      final SimpleTest ts = new SimpleTest("This is a test", 2);
      //noinspection InfiniteLoopStatement
      for (; ; ) {
        try {
          ts.message(ts.newId());
        } catch (Exception e) {
          //noinspection CallToPrintStackTrace
          e.printStackTrace();
        }

        double duration = Math.abs(random.nextGaussian() / 10.0 + 0.2);
        String status = random.nextInt(100) < 20 ? "500" : "200";
        histogram.labelValues("/", status).observe(duration);

        //noinspection BusyWait
        Thread.sleep(1000);
      }
    }
  }
}
