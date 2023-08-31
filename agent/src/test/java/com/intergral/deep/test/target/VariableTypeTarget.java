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

package com.intergral.deep.test.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ALL")
public class VariableTypeTarget extends VariableSuperTest {

  public static int i = 1;
  protected static float f = 2.2f;
  static long l = 2L;
  private static double d = 1.2d;
  private final Object object = new Object();
  public String str =
      "str with a very large value will be truncated, needs to be over 1024 by default.............................................................................................................................................................................................................................................................................................................................................................................................................................................................................."
          + "str with a very large value will be truncated, needs to be over 1024 by default.............................................................................................................................................................................................................................................................................................................................................................................................................................................................................."
          + "str with a very large value will be truncated, needs to be over 1024 by default..............................................................................................................................................................................................................................................................................................................................................................................................................................................................................";
  protected String[] arry = new String[]{"some", "thing"};
  Object nul = null;
  private volatile List<String> list = new ArrayList<>();
  private transient Set<String> set = new HashSet<>(Arrays.asList("one", "two"));

  private Map<String, String> simplemap = new HashMap<>(Collections.singletonMap("key", "value"));
  private Map<String, Object> complexMap = new HashMap<String, Object>() {{
    put("key", "other");
    put("1", 1);
    put("list", list);
  }};

  private Iterator<?> iter = complexMap.entrySet().iterator();

  private VariableTypeTarget someObj = this;

  public VariableTypeTarget() {
    // this is here to ensure we have a line to install TP on for tests
    nul = null;
  }

  @Override
  public String toString() {
    return "VariableTypeTarget{}";
  }
}
