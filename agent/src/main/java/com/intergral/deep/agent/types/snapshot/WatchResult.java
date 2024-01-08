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

package com.intergral.deep.agent.types.snapshot;

/**
 * The result of a watch expression evaluation.
 */
public class WatchResult {

  /**
   * Watch source is METRIC.
   *
   * @see com.intergral.deep.proto.tracepoint.v1.WatchSource
   */
  public static final String METRIC = "METRIC";

  /**
   * Watch source is WATCH.
   *
   * @see com.intergral.deep.proto.tracepoint.v1.WatchSource
   */
  public static final String WATCH = "WATCH";

  /**
   * Watch source is LOG.
   *
   * @see com.intergral.deep.proto.tracepoint.v1.WatchSource
   */
  public static final String LOG = "LOG";

  /**
   * Watch source is CAPTURE.
   *
   * @see com.intergral.deep.proto.tracepoint.v1.WatchSource
   */
  public static final String CAPTURE = "CAPTURE";

  private final String error;
  private final VariableID result;
  private final String expression;
  private final String source;


  /**
   * Create a bad result.
   *
   * @param expression the expression
   * @param error      the error
   * @param source     the source of this result
   */
  public WatchResult(final String expression, final String error, final String source) {
    this.expression = expression;
    this.error = error;
    this.result = null;
    this.source = source;
  }

  /**
   * Create a good result.
   *
   * @param expression the expression
   * @param result     the result
   * @param source     the source of this result
   */
  public WatchResult(final String expression, final VariableID result, final String source) {
    this.expression = expression;
    this.source = source;
    this.error = null;
    this.result = result;
  }

  public boolean isError() {
    return this.error != null;
  }

  public String error() {
    return this.error;
  }

  public VariableID goodResult() {
    return result;
  }

  public String expression() {
    return this.expression;
  }

  public String getSource() {
    return source;
  }
}
