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
import com.intergral.deep.agent.api.reflection.IReflection;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    System.setProperty("deep.jar.path", jarPath.toString());

    Deep.config()
        .setValue("service.url", "localhost:43315")
        .setValue("service.secure", false)
        .start();

    final Deep instance = Deep.getInstance();
    System.out.println(instance.<IDeep>api().getVersion());
    System.out.println(instance.<IReflection>reflection());

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
