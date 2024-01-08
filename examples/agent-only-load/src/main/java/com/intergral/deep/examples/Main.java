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



/**
 * This example expects the deep agent to be loaded via the javaagent vm option.
 * <p>
 * See RunConfigurations for IDEA:
 * <ul>
 *   <li>Agent ONLY Load with JavaAgent</li>
 * </ul>
 */
public class Main {

  /**
   * Main entry for example.
   *
   * @param args the startup arguments
   * @throws Throwable if we error
   */
  public static void main(String[] args) throws Throwable {

    final SimpleTest ts = new SimpleTest("This is a test", 2);
    //noinspection InfiniteLoopStatement
    for (; ; ) {
      try {
        ts.message(ts.newId());
      } catch (Exception e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
      }

      //noinspection BusyWait
      Thread.sleep(1000);
    }
  }
}
