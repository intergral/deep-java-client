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


import com.intergral.deep.DEEPAPI;
import com.intergral.deep.Deep;
import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.resource.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

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

  public static void main(String[] args) throws Throwable {
    // this is only needed in this example as we are using a local built module
    // if using the dependency from maven you do not need to set the path
    Path jarPath = Paths.get(Main.class.getResource("/").toURI())
        .getParent()
        .getParent()
        .getParent()
        .getParent()
        .resolve("agent/target/deep-1.0-SNAPSHOT.jar");

    // Dynamically configure and attach the deep agent
    Deep.config()
        .setJarPath(jarPath.toAbsolutePath().toString())
        .setValue("service.url", "localhost:43315")
        .setValue("service.secure", false)
        .start();

    // different ways to get the API instance
    final Deep instance = Deep.getInstance();
    System.out.println(instance.<IDeep>api().getVersion());
    System.out.println(instance.<IReflection>reflection());

    System.out.println(DEEPAPI.api().getVersion());
    System.out.println(DEEPAPI.reflection());

    // Use the API to register a plugin
    // This plugin will attach the attribute 'example' to the created snapshot
    // you should also see the log line 'custom plugin' when you run this example
    DEEPAPI.api().registerPlugin((settings, snapshot) -> {
      System.out.println("custom plugin");
      return Resource.create(Collections.singletonMap("example", "dynamic_load"));
    });

    // USe the API to create a tracepoint that will fire forever
    DEEPAPI.api()
        .registerTracepoint("com/intergral/deep/examples/SimpleTest", 46, Collections.singletonMap("fire_count", "-1"), Collections.emptyList());

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
