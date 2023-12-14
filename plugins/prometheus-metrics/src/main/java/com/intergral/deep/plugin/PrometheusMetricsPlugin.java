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

package com.intergral.deep.plugin;

import com.intergral.deep.agent.api.plugin.IMetricProcessor;
import com.intergral.deep.agent.api.spi.IConditional;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.api.spi.Ordered;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Counter.Builder;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This plugin provides the ability to post tracepoint generated metric to prometheus.
 * <p>
 * This plugin is loaded by the Deep module and not the agent. As we need to be loaded in the app class path and not the boot class path.
 */
public class PrometheusMetricsPlugin implements IDeepPlugin, IConditional, IMetricProcessor, Ordered {

  private static final Map<String, Object> REGISTRY_CACHE = new ConcurrentHashMap<>();

  @Override
  public boolean isActive() {
    try {
      // just trying to see if the prometheus classes are loaded
      final PrometheusRegistry ignored = PrometheusRegistry.defaultRegistry;
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  @Override
  public void counter(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final String ident = buildIdent("counter", namespace, name, labels.keySet());
    Object o = REGISTRY_CACHE.get(ident);
    if (o == null) {

      final Builder builder = Counter.builder().name(String.format("%s_%s", namespace, name)).help(help);
      if (!labels.isEmpty()) {
        builder.labelNames(Arrays.toString(labels.keySet().toArray()));
      }
      final Counter register = builder.register();
      REGISTRY_CACHE.put(ident, register);

      o = register;
    }

    if (!labels.isEmpty()) {
      ((Counter) o).labelValues(Arrays.toString(labels.values().toArray())).inc(value);
    } else {
      ((Counter) o).inc(value);
    }
  }

  @Override
  public void gauge(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final String ident = buildIdent("gauge", namespace, name, labels.keySet());
    Object o = REGISTRY_CACHE.get(ident);
    if (o == null) {
      final Gauge.Builder builder = Gauge.builder().name(String.format("%s_%s", namespace, name)).help(help);
      if (!labels.isEmpty()) {
        builder.labelNames(Arrays.toString(labels.keySet().toArray()));
      }
      final Gauge register = builder.register();
      REGISTRY_CACHE.put(ident, register);

      o = register;
    }

    if (!labels.isEmpty()) {
      ((Gauge) o).labelValues(Arrays.toString(labels.values().toArray())).set(value);
    } else {
      ((Gauge) o).set(value);
    }
  }

  @Override
  public void histogram(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final String ident = buildIdent("histogram", namespace, name, labels.keySet());
    Object o = REGISTRY_CACHE.get(ident);
    if (o == null) {
      final Histogram.Builder builder = Histogram.builder().name(String.format("%s_%s", namespace, name)).help(help);
      if (!labels.isEmpty()) {
        builder.labelNames(Arrays.toString(labels.keySet().toArray()));
      }
      final Histogram register = builder.register();
      REGISTRY_CACHE.put(ident, register);
      o = register;
    }

    if (!labels.isEmpty()) {
      ((Histogram) o).labelValues(Arrays.toString(labels.values().toArray())).observe(value);
    } else {
      ((Histogram) o).observe(value);
    }
  }

  @Override
  public void summary(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final String ident = buildIdent("summary", namespace, name, labels.keySet());
    Object o = REGISTRY_CACHE.get(ident);
    if (o == null) {
      final Summary.Builder builder = Summary.builder().name(String.format("%s_%s", namespace, name)).help(help);
      if (!labels.isEmpty()) {
        builder.labelNames(Arrays.toString(labels.keySet().toArray()));
      }
      final Summary register = builder.register();
      REGISTRY_CACHE.put(ident, register);
      o = register;
    }

    if (!labels.isEmpty()) {
      ((Summary) o).labelValues(Arrays.toString(labels.values().toArray())).observe(value);
    } else {
      ((Summary) o).observe(value);
    }
  }

  private String buildIdent(final String type, final String namespace, final String name, final Set<String> labelNames) {
    return String.format("%s_%s_%s_%s", type, namespace, name, String.join("-", labelNames));
  }
}
