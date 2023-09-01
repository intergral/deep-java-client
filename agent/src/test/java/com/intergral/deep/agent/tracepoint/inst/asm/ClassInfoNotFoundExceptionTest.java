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

package com.intergral.deep.agent.tracepoint.inst.asm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClassInfoNotFoundExceptionTest {

  @Test
  void coverage() {
    assertEquals("some test", new ClassInfoNotFoundException("some test", "some type").getMessage());
    assertEquals("some type", new ClassInfoNotFoundException("some test", "some type").getType());
    assertEquals("some cause",
        new ClassInfoNotFoundException("some test", "some type", new RuntimeException("some cause")).getCause().getMessage());
  }
}