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

package com.intergral.deep.agent.api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompoundIteratorTest {

  @Test
  void next() {
    final CompoundIterator<String> iterator = new CompoundIterator<>(new ArrayIterator<>(new String[]{"one", "two", "three"}),
        new ArrayIterator<>(new String[]{"one_1", "two_2", "three_3"}));

    assertTrue(iterator.hasNext());
    assertEquals("one", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("two", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("three", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("one_1", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("two_2", iterator.next());
    assertTrue(iterator.hasNext());
    assertEquals("three_3", iterator.next());
    assertFalse(iterator.hasNext());
  }
}