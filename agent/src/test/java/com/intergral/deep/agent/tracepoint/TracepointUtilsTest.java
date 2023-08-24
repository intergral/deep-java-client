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

package com.intergral.deep.agent.tracepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intergral.deep.test.MockTracepointConfig;
import org.junit.jupiter.api.Test;

class TracepointUtilsTest {

  @Test
  void estimatedClassRoot() {
    assertEquals("com/intergral/class/name",
        TracepointUtils.estimatedClassRoot(new MockTracepointConfig().withArg("class_name", "com.intergral.class.name")));
    assertEquals("some", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("some.java")));
    assertEquals("src/some", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/src/some.java")));
    assertEquals("some", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/src/some.java").withArg("src_root", "/src")));
    assertEquals("some", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("some.java").withArg("src_root", "src/main/java")));

    assertEquals("cfm", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("some.cfm").withArg("src_root", "src/")));
    assertEquals("cfm", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("some.cfc").withArg("src_root", "src/")));

    assertEquals("jsp", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("some.jsp").withArg("src_root", "src/")));

    assertEquals("hello/world/HelloController", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/src/main/java/hello/world/HelloController.java").withArg("src_root", "/src/main/java")));
    assertEquals("hello/world/HelloController", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/src/main/java/hello/world/HelloController.java")));

    assertEquals("docker/Dockerfile", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/docker/Dockerfile")));
    assertEquals(".gitignore", TracepointUtils.estimatedClassRoot(new MockTracepointConfig("/.gitignore")));
  }
}