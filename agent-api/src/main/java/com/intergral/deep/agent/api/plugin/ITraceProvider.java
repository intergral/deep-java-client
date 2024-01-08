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

package com.intergral.deep.agent.api.plugin;

/**
 * This type can be used to connect Deep to a trace provider. This will allow Deep to create Spans dynamically based on the config of a
 * tracepoint.
 */
public interface ITraceProvider {

  /**
   * Create and return a new span.
   *
   * @param name the name of the span to create
   * @return the new span
   */
  ISpan createSpan(final String name);

  /**
   * Get the current span from the underlying provider.
   *
   * @return the current active span, or {@code null}
   */
  ISpan currentSpan();

  /**
   * This type describes a span for Deep to use as a dynamic Span, it gives a common interface for all {@link ITraceProvider}'s.
   */
  interface ISpan extends AutoCloseable {
    String name();

    String traceId();

    String spanId();

    void addAttribute(String key, String value);
  }
}
