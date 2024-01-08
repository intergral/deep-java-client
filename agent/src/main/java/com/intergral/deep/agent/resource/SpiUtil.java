/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.spi.Ordered;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to load SPI services.
 */
public final class SpiUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpiUtil.class);

  private SpiUtil() {
  }

  public static <T extends Ordered> List<T> loadOrdered(Class<T> spiClass,
      ClassLoader serviceClassLoader) {
    return loadOrdered(spiClass, serviceClassLoader, ServiceLoader::load);
  }

  // VisibleForTesting
  static <T extends Ordered> List<T> loadOrdered(
      Class<T> spiClass, ClassLoader serviceClassLoader, ServiceLoaderFinder serviceLoaderFinder) {
    List<T> result = new ArrayList<>();
    final Iterator<T> iterator = serviceLoaderFinder.load(spiClass, serviceClassLoader).iterator();
    while (iterator.hasNext()) {
      try {
        result.add(iterator.next());
      } catch (Throwable t) {
        // WARNING - exception from this will have the wrong stack trace
        // the error will come from 'hasNext' not 'next'
        LOGGER.error("Cannot load provider.", t);
      }
    }
    result.sort(Comparator.comparing(Ordered::order));
    return result;
  }

  interface ServiceLoaderFinder {

    <S> Iterable<S> load(Class<S> spiClass, ClassLoader classLoader);
  }
}
