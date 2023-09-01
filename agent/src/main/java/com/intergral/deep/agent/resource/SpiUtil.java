/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.spi.Ordered;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Utilities to load SPI services.
 */
public final class SpiUtil {

  private SpiUtil() {
  }

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
