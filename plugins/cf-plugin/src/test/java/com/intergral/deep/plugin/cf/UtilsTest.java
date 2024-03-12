/*
 *     Copyright (C) 2024  Intergral GmbH
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

package com.intergral.deep.plugin.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

class UtilsTest {

  @Test
  void canDetectNotCF() {
    assertFalse(Utils.isCFServer());
  }

  @Test
  @ClearSystemProperty(key = "sun.java.command")
  void canHandleNoSunCommand() {
    assertFalse(Utils.isCFServer());
  }

  @Test
  @SetSystemProperty(key = "coldfusion.home", value = "doesn't matter")
  void canDetectCFHome() {
    assertTrue(Utils.isCFServer());
  }

  @Test
  @SetSystemProperty(key = "sun.java.command", value = "/some/coldfusion")
  void canUseSunCommand() {
    assertTrue(Utils.isCFServer());
  }

  @Test
  void loadCFVersion() {
    assertEquals("10", Utils.loadCFVersion());
  }

  @Test
  @SetSystemProperty(key = "cf.test.error", value = "anything")
  void failLoadCFVersion() {
    assertNull(Utils.loadCFVersion());
  }
}