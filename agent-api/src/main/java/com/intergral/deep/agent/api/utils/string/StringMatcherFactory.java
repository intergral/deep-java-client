/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package com.intergral.deep.agent.api.utils.string;

public class StringMatcherFactory {


  /**
   * Defines the singleton for this class.
   */
  public static final StringMatcherFactory INSTANCE = new StringMatcherFactory();

  /**
   * Matches no characters.
   */
  private static final AbstractStringMatcher.NoMatcher NONE_MATCHER = new AbstractStringMatcher.NoMatcher();

  /**
   * Creates a matcher from a string.
   *
   * @param str
   *            the string to match, null or empty matches nothing
   * @return a new Matcher for the given String
   */
  public StringMatcher stringMatcher(final String str) {
    if (str == null || str.isEmpty()) {
      return NONE_MATCHER;
    }
    return new AbstractStringMatcher.StringMatcher(str);
  }

  /**
   * Constructor that creates a matcher from a character.
   *
   * @param ch
   *            the character to match, must not be null
   * @return a new Matcher for the given char
   */
  public StringMatcher charMatcher(final char ch) {
    return new AbstractStringMatcher.CharMatcher(ch);
  }
}
