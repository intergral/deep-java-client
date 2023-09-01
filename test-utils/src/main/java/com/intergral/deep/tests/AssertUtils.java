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

package com.intergral.deep.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

public class AssertUtils {

  /**
   * Assert that a collection contains an item that matches the function.
   *
   * @param list            the collection to scan
   * @param compareFunction the function to run
   * @param <T>             the type of items in the collection
   * @return the result of the compare
   */
  public static <T> int assertContains(final Collection<T> list, final ICompareFunction<T> compareFunction) {
    int index = -1;
    for (final T listItem : list) {
      index++;
      if (compareFunction.compare(listItem)) {
        return index;
      }
    }
    fail(String.format("Cannot find %s in list %s", compareFunction, list));
    return -1;
  }

  public interface ICompareFunction<T> {

    boolean compare(T item);
  }
}
