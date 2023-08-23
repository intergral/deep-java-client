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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Utils {

  /**
   * Get the current version of Java running in this JVM
   *
   * @return the java version number
   */
  public static int getJavaVersion() {
    String version = System.getProperty("java.version");
    return extractVersion(version);
  }

  static int extractVersion(String version) {
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }


  /**
   * Get the current time in mills and  nanoseconds from epoch
   * <p>
   * Long will wrap when the date exceeds Saturday, 12 April 2262 00:47:16.854 GMT+01:00 DST
   *
   * @return the epoch time in mills and nanos
   */
  public static long[] currentTimeNanos() {
    final Instant now = Instant.now();
    final String format = String.format("%d%d", now.getEpochSecond(), now.getNano());
    return new long[]{now.toEpochMilli(), Long.parseLong(format)};
  }

  /**
   * Create a new map from the input
   *
   * @param map the input map
   * @param <T> the key type
   * @return a new map with the same values are the input, or a new empty map
   */
  public static <T> Map<String, T> newMap(final Map<String, T> map) {
    if (map == null) {
      return Collections.emptyMap();
    }
    return new HashMap<>(map);
  }


  /**
   * FROM: <a href="https://stackoverflow.com/a/38947571">view source</a>
   *
   * @param str    the string to search
   * @param suffix the value to serch for
   * @return true if {@code str} ends with {@code suffix}, disregarding case sensitivity
   */
  public static boolean endsWithIgnoreCase(String str, String suffix) {
    int suffixLength = suffix.length();
    return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
  }

  /**
   * This will create a string representation of the object passed in.
   *
   * @param obj the value to create a string from
   * @return the string form of the object
   */
  public static String valueOf(final Object obj) {
    if (obj == null) {
      return "null";
    }

    String hash;
    // sometimes (on bad objects) .toString will fail. So we need to protect against that.
    try {
      return String.valueOf(obj);
    } catch (final Throwable e1) {
      // From Object.toString();
      hash = String.valueOf(System.identityHashCode(obj));
      return obj.getClass().getName() + "@" + hash + " toString() failed";
    }
  }


  /**
   * Trim a string from another string
   *
   * @param str     the target string
   * @param prefix the value to remove from the string
   * @return the new string
   */
  public static String trimPrefix(String str, final String prefix) {
    while (str.startsWith(prefix)) {
      str = str.substring(1);
    }
    return str;
  }


  /**
   * Trim a string to a specified length
   *
   * @param str       the target string
   * @param maxLength the max length to make the string
   * @return a {@link ITrimResult}, so we can know if the string was trimmed
   */
  public static ITrimResult truncate(final String str, final int maxLength) {
    if (str.length() > maxLength) {
      return new ITrimResult() {
        @Override
        public String value() {
          return str.substring(0, maxLength);
        }


        @Override
        public boolean truncated() {
          return true;
        }
      };
    }
    return new ITrimResult() {
      @Override
      public String value() {
        return str;
      }


      @Override
      public boolean truncated() {
        return false;
      }
    };
  }


  /**
   * The result of a trim operation
   */
  public interface ITrimResult {

    /**
     * The value to use, might be truncated
     *
     * @return the value
     */
    String value();


    /**
     * Has the value been truncated
     *
     * @return {@code true} if the value was truncated
     */
    boolean truncated();
  }
}
