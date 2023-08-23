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

package com.intergral.deep.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void javaVersion() {
    assertTrue(Utils.getJavaVersion() > 7);
  }

  @Test
  void currentTimeNanos() throws InterruptedException {
    final long[] start = Utils.currentTimeNanos();
    Thread.sleep(1000);
    final long[] after = Utils.currentTimeNanos();

    assertTrue(after[0] > start[0]);
    assertTrue(after[1] > start[1]);
  }

  @Test
  void newMap() {
    assertEquals(Collections.emptyMap(), Utils.newMap(new HashMap<>()));

    assertEquals("value", Utils.newMap(Collections.singletonMap("key", "value")).get("key"));
  }

  @Test
  void endsWithIgnoreCase() {
    assertTrue(Utils.endsWithIgnoreCase("someString", "string"));
    assertTrue(Utils.endsWithIgnoreCase("someString", "String"));
    assertFalse(Utils.endsWithIgnoreCase("someString", "qtring"));
  }

  @Test
  void valueOf() {
    class badtostring {

      @Override
      public String toString() {
        throw new NullPointerException();
      }

    }
    assertEquals("some value", Utils.valueOf("some value"));
    assertEquals("1", Utils.valueOf("1"));
    assertEquals("null", Utils.valueOf(null));
    final String valueOf = Utils.valueOf(new badtostring());
    assertNotNull(valueOf);
    assertTrue(valueOf.startsWith(badtostring.class.getName()));
    assertTrue(valueOf.endsWith("toString() failed"));
  }

  @Test
  void trim() {
    assertEquals("value", Utils.trimPrefix("//value", "/"));
  }

  @Test
  void truncate() {
    assertTrue(Utils.truncate("somelongstring", 10).truncated());
    assertEquals("somelongst", Utils.truncate("somelongstring", 10).value());
    assertFalse(Utils.truncate("somelongstring", 20).truncated());
    assertEquals("somelongstring", Utils.truncate("somelongstring", 20).value());
  }
}