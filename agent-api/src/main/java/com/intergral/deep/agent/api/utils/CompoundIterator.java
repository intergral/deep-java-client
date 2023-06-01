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

package com.intergral.deep.agent.api.utils;

import java.util.Iterator;

public class CompoundIterator<T> implements Iterator<T> {

  private final Iterator<T>[] iterators;
  private int currentIterator = 0;

  @SafeVarargs
  public CompoundIterator(final Iterator<T>... iterators) {
    this.iterators = iterators;
  }

  @Override
  public boolean hasNext() {
    checkCurrent();
    return getCurrent().hasNext();
  }

  private Iterator<T> getCurrent() {
    if (currentIterator < iterators.length) {
      return iterators[currentIterator];
    }
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public T next() {
        return null;
      }
    };
  }

  @Override
  public T next() {
    final T next = getCurrent().next();
    checkCurrent();
    return next;
  }

  private void checkCurrent() {
    final Iterator<T> current = getCurrent();
    if (current.hasNext()) {
      return;
    }
    currentIterator++;
  }
}
