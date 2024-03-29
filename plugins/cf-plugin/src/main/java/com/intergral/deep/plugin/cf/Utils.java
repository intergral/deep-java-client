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

package com.intergral.deep.plugin.cf;

/**
 * A small collection of utils used to capture the adobe coldfusion version number.
 */
public final class Utils {

  private Utils() {
  }

  /**
   * Are we running on a CF server.
   * <p>
   * By checking that the {@code coldfusion.home} system property exists, or by looking at the java start up command,
   * we can tell if this is a CF server.
   *
   * @return {@code true} if we are on a coldfusion server.
   */
  public static boolean isCFServer() {
    if (System.getProperty("coldfusion.home") != null) {
      return true;
    }

    final String javaCommand = System.getProperty("sun.java.command");
    // has the potential to not exist on Windows services
    if (javaCommand == null) {
      return false;
    }
    return javaCommand.contains("coldfusion");
  }

  /**
   * Try to load the coldfusion version number.
   *
   * @return the major version of adobe coldfusion.
   */
  public static String loadCFVersion() {
    try {
      return String.valueOf(Thread.currentThread()
          .getContextClassLoader()
          .loadClass("coldfusion.Version")
          .getMethod("getMajor")
          .invoke(null));
    } catch (Exception e) {
      return null;
    }
  }
}
