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

package com.intergral.deep.tests.it.java;

import java.util.function.IntFunction;

public class BP2TestTarget {

  public static String staticMethodTest(final String name) {
    final BPTestTarget bpTestTarget = new BPTestTarget(name);
    return bpTestTarget.getName();
  }


  public static String staticInnerClass() {
    return new IntFunction<String>() {
      @Override
      public String apply(final int value) {
        return new BPTestTarget(String.valueOf(value)).getName();
      }
    }.apply(2);
  }


  public static String staticLambdaClass(final String val) {
    return ((IntFunction<String>) value -> {
      return new BPTestTarget(value + val).getName();
    }).apply(2);
  }


  public static String staticLambdaClass2(final String val) {
    return ((IntFunction<String>) value -> new BPTestTarget(value + val).getName()).apply(2);
  }
}
