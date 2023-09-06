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


import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.api.DeepAPI;
import java.util.Collections;

/**
 * This example expects the deep agent to be loaded via the javaagent vm option.
 * <p>
 * See RunConfigurations for IDEA:
 * <ul>
 *   <li>Agent Load without JavaAgent</li>
 *   <li>Agent Load with JavaAgent</li>
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

    // Use the API to show the version of deep that is running
    // If the Deep agent is not loaded then you will get an exception on this line
    // java.lang.IllegalStateException: Must start Deep first!
    System.out.println(DeepAPI.api().getVersion());

    // Use the API to register a plugin
    // This plugin will attach the attribute 'example' to the created snapshot
    // you should also see the log line 'custom plugin' when you run this example
    DeepAPI.api().registerPlugin((settings, snapshot) -> {
      System.out.println("custom plugin");
      return Resource.create(Collections.singletonMap("example", "agent_load"));
    });

    // USe the API to create a tracepoint that will fire forever
    DeepAPI.api()
        .registerTracepoint("com/intergral/deep/examples/SimpleTest", 46, Collections.singletonMap("fire_count", "-1"),
            Collections.emptyList());

    final SimpleTest ts = new SimpleTest("This is a test", 2);
    for (; ; ) {
      try {
        ts.message(ts.newId());
      } catch (Exception e) {
        e.printStackTrace();
      }

      Thread.sleep(1000);
    }
  }
}
