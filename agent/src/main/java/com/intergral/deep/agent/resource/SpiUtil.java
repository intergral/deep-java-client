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

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.spi.Ordered;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class SpiUtil {

  static <T extends Ordered> List<T> loadOrdered(Class<T> spiClass,
      ClassLoader serviceClassLoader) {
    return loadOrdered(spiClass, serviceClassLoader, ServiceLoader::load);
  }

  // VisibleForTesting
  static <T extends Ordered> List<T> loadOrdered(
      Class<T> spiClass, ClassLoader serviceClassLoader, ServiceLoaderFinder serviceLoaderFinder) {
    List<T> result = new ArrayList<>();
    for (T service : serviceLoaderFinder.load(spiClass, serviceClassLoader)) {
      result.add(service);
    }
    result.sort(Comparator.comparing(Ordered::order));
    return result;
  }

  interface ServiceLoaderFinder {

    <S> Iterable<S> load(Class<S> spiClass, ClassLoader classLoader);
  }
}
