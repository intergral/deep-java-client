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

import com.intergral.deep.agent.api.DeepVersion;
import com.intergral.deep.agent.api.plugin.IMetricProcessor;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.plugin.ISnapshotDecorator;
import com.intergral.deep.agent.api.plugin.ITraceProvider;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IConditional;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.HashMap;
import java.util.Map;

public class OtelPlugin implements IDeepPlugin, ITraceProvider, IMetricProcessor, IConditional, ISnapshotDecorator {

  @Override
  public void counter(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final MeterProvider meterProvider = GlobalOpenTelemetry.getMeterProvider();
    final Meter deep = meterProvider.get("deep");
    final LongCounter build = deep.counterBuilder(name).setUnit(unit).setDescription(help).build();
    build.add(value.longValue());
  }

  @Override
  public void gauge(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final MeterProvider meterProvider = GlobalOpenTelemetry.getMeterProvider();
    final Meter deep = meterProvider.get("deep");
    final ObservableDoubleMeasurement observableDoubleMeasurement = deep.gaugeBuilder(name).setUnit(unit).setDescription(help)
        .buildObserver();
    observableDoubleMeasurement.record(value);
  }

  @Override
  public void histogram(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    final MeterProvider meterProvider = GlobalOpenTelemetry.getMeterProvider();
    final Meter deep = meterProvider.get("deep");
    final DoubleHistogram build = deep.histogramBuilder(name).setUnit(unit).setDescription(help).build();
    build.record(value);
  }

  @Override
  public void summary(final String name, final Map<String, Object> labels, final String namespace, final String help, final String unit,
      final Double value) {
    histogram(name, labels, namespace, help, unit, value);
  }

  @Override
  public ISpan createSpan(final String name) {
    final TracerProvider tracerProvider = GlobalOpenTelemetry.getTracerProvider();
    if (tracerProvider == null) {
      return null;
    }

    final Tracer deep = tracerProvider.get("deep", DeepVersion.VERSION);
    if (deep == null) {
      return null;
    }

    final Span span = deep.spanBuilder(name).setAttribute("deep", DeepVersion.VERSION).startSpan();
    return new ISpan() {
      @Override
      public String name() {
        return name;
      }


      @Override
      public String traceId() {
        if (span != null) {
          return span.getSpanContext().getTraceId();
        }
        return null;
      }

      @Override
      public String spanId() {
        if (span != null) {
          return span.getSpanContext().getSpanId();
        }
        return null;
      }

      @Override
      public void close() throws Exception {
        try {
          span.end();
        } catch (Throwable ignored) {

        }
      }
    };
  }

  @Override
  public ISpan currentSpan() {
    final Span current = Span.current();
    final ReadableSpan readableSpan;
    if (current instanceof ReadableSpan) {
      readableSpan = (ReadableSpan) current;
    } else {
      readableSpan = null;
    }
    if (current == null) {
      return null;
    }
    return new ISpan() {
      @Override
      public String name() {
        if (readableSpan != null) {
          return readableSpan.getName();
        }
        return "unknown";
      }


      @Override
      public String traceId() {
        return current.getSpanContext().getTraceId();
      }

      @Override
      public String spanId() {
        return current.getSpanContext().getSpanId();
      }

      @Override
      public void close() {
        throw new IllegalStateException("Cannot close external spans.");
      }
    };
  }

  @Override
  public boolean isActive() {
    try {
      final Class<GlobalOpenTelemetry> ignored = GlobalOpenTelemetry.class;
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  @Override
  public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
    final Map<String, Object> map = new HashMap<>();
    final ISpan iSpan = this.currentSpan();
    if (iSpan != null) {
      map.put("trace_id", iSpan.traceId());
      map.put("span_id", iSpan.spanId());
    }
    return Resource.create(map);
  }
}
