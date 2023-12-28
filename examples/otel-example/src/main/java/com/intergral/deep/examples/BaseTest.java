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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class BaseTest {

  protected final Properties systemProps = System.getProperties();


  public String newId() {
    return UUID.randomUUID().toString();
  }


  public Map<Character, Integer> makeCharCountMap(final String str) {
    final HashMap<Character, Integer> res = new HashMap<Character, Integer>();

    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      final Integer cnt = res.get(c);
      if (cnt == null) {
        res.put(c, 0);
      } else {
        res.put(c, cnt + 1);
      }
    }

    return res;
  }
}
