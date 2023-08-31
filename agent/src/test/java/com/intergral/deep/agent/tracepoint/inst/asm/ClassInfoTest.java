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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lucee.loader.classloader.LuceeClassLoader;
import org.junit.jupiter.api.Test;
import railo.loader.classloader.RailoClassLoader;

class ClassInfoTest {

  @Test
  void isLuceeClassLoader() {
    assertTrue(ClassInfo.isLuceeClassLoader(new LuceeClassLoader()));
    assertFalse(ClassInfo.isRailoClassLoader(new LuceeClassLoader()));
  }

  @Test
  void isRailoClassLoader() {
    assertFalse(ClassInfo.isLuceeClassLoader(new RailoClassLoader()));
    assertTrue(ClassInfo.isRailoClassLoader(new RailoClassLoader()));

  }

  @Test
  void isSafeClassLoader() {
    assertFalse(ClassInfo.isSafeLoader(new RailoClassLoader()));
    assertFalse(ClassInfo.isSafeLoader(new LuceeClassLoader()));
    assertTrue(ClassInfo.isSafeLoader(new railo.commons.lang.PCLBlock()));
    assertTrue(ClassInfo.isSafeLoader(new lucee.commons.lang.PCLBlock()));
  }
}