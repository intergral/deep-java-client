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

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentTest {


  @Test
  void parseArgs() {
    final Map<String, String> stringStringMap = Agent.parseArgs("me=you");
    assertEquals(stringStringMap, Collections.singletonMap("me", "you"));
  }


  @Test
  void parseArgsWithTags() {
    final Map<String, String> stringStringMap = Agent.parseArgs("tags=testKey=testVal;testkey2=testval2");
    assertEquals(stringStringMap, Collections.singletonMap("tags", "testKey=testVal;testkey2=testval2"));
  }
}