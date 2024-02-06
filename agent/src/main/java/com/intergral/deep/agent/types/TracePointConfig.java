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

package com.intergral.deep.agent.types;

import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.settings.Settings;
import java.util.Collection;
import java.util.Map;

/**
 * This type defines a tracepoint configuration.
 */
public class TracePointConfig {

  /**
   * Collect only the frame we are on.
   */
  public static final String SINGLE_FRAME_TYPE = "single_frame";
  /**
   * Collect from all available frames.
   */
  public static final String ALL_FRAME_TYPE = "all_frame";
  /**
   * Collect on frame data.
   */
  public static final String NO_FRAME_TYPE = "no_frame";
  /**
   * Collect the full stack.
   */
  public static final String STACK = "stack";
  /**
   * The condition that has to be 'truthy' for this tracepoint to fire.
   */
  public static final String CONDITION = "condition";
  /**
   * The number of times this tracepoint should fire.
   */
  public static final String FIRE_COUNT = "fire_count";
  /**
   * The start of the time period this tracepoint can fire in.
   */
  private static final String WINDOW_START = "window_start";
  /**
   * The end of the time period this tracepoint can fire in.
   */
  private static final String WINDOW_END = "window_end";
  /**
   * The minimum time between successive triggers, in ms.
   */
  public static final String FIRE_PERIOD = "fire_period";
  /**
   * This is the key to indicate the frame collection type.
   */
  private static final String FRAME_TYPE = "frame_type";
  /**
   * This is the key to indicate the stack collection type.
   */
  private static final String STACK_TYPE = "stack_type";

  /**
   * The log message to interpolate at position of tracepoint.
   */
  public static final String LOG_MSG = "log_msg";

  /**
   * This is the key for the arg that defines a method tracepoint.
   */
  public static final String METHOD_NAME = "method_name";

  /**
   * This is the key for the arg that defines a span type.
   */
  public static final String SPAN = "span";

  /**
   * This is used for SPAN type.
   * <p>
   * This type means we should wrap the method the tracepoint is in.
   */
  public static final String METHOD = "method";
  /**
   * This is used for SPAN type.
   * <p>
   * This type means we should only wrap the line the tracepoint is on.
   */
  @SuppressWarnings("unused")
  public static final String LINE = "line";

  /**
   * This is the key to determine the collection state of the snapshot.
   */
  public static final String SNAPSHOT = "snapshot";

  /**
   * This is the default collection type and tells Deep to collect and send the snapshot.
   */
  public static final String COLLECT = "collect";

  /**
    * This type tells Deep to not collect any data and not to send the snapshot.
   */
  public static final String NO_COLLECT = "no_collect";

  public static final String STAGE = "stage";
  public static final String LINE_START = "line_start";
  public static final String LINE_END = "line_end";
  public static final String LINE_CAPTURE = "line_capture";
  public static final String METHOD_START = "method_start";
  public static final String METHOD_END = "method_end";
  public static final String METHOD_CAPTURE = "method_capture";

  private final String id;
  private final String path;
  private final int lineNo;
  private final Map<String, String> args;
  private final Collection<String> watches;
  private final TracepointWindow window;

  private final TracepointExecutionStats stats = new TracepointExecutionStats();
  private final Collection<MetricDefinition> metricDefinitions;


  /**
   * Create a new tracepoint config.
   *
   * @param id                the id
   * @param path              the path
   * @param lineNo            the line number
   * @param args              the args
   * @param watches           the watches
   * @param metricDefinitions the metrics to evaluate
   */
  public TracePointConfig(
      final String id,
      final String path,
      final int lineNo,
      final Map<String, String> args,
      final Collection<String> watches,
      final Collection<MetricDefinition> metricDefinitions) {
    this.id = id;
    this.path = path;
    this.lineNo = lineNo;
    this.args = args;
    this.watches = watches;
    this.metricDefinitions = metricDefinitions;
    this.window = new TracepointWindow(this.getArg(WINDOW_START, Integer.class, 0),
        this.getArg(WINDOW_END, Integer.class, 0));
  }

  /**
   * Get the ordinal for the frame type to allow for sorting.
   *
   * @param frameType the frame type
   * @return the ordinal for the frame type
   */
  public static int frameTypeOrdinal(String frameType) {
    switch (frameType) {
      case ALL_FRAME_TYPE:
        return 2;
      case NO_FRAME_TYPE:
        return 0;
      case SINGLE_FRAME_TYPE:
      default:
        return 1;
    }
  }

  /**
   * Get the tracepoint id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Get the tracepoint path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Get the tracepoint line number.
   *
   * @return the line number
   */
  public int getLineNo() {
    return lineNo;
  }

  /**
   * Get the tracepoint args.
   *
   * @return the args
   */
  public Map<String, String> getArgs() {
    return args;
  }

  /**
   * Get the tracepoint watches.
   *
   * @return the watches
   */
  public Collection<String> getWatches() {
    return watches;
  }

  /**
   * Get the tracepoint metric definitions.
   *
   * @return the metric definitions
   */
  public Collection<MetricDefinition> getMetricDefinitions() {
    return metricDefinitions;
  }

  /**
   * Get the tracepoint fire count.
   *
   * @return the fire count
   */
  public int getFireCount() {
    return getArg(FIRE_COUNT, Integer.class, 1);
  }

  /**
   * Get the tracepoint condition.
   *
   * @return the tracepoint condition or, {@code null}
   */
  public String getCondition() {
    return getArg(CONDITION, String.class, null);
  }


  /**
   * Get the tracepoint frame type.
   *
   * @return the tracepoint frame type
   */
  public String getFrameType() {
    return getArg(FRAME_TYPE, String.class, SINGLE_FRAME_TYPE);
  }

  /**
   * Get the tracepoint stack type.
   *
   * @return the tracepoint stack type
   */
  public String getStackType() {
    return getArg(STACK_TYPE, String.class, STACK);
  }

  /**
   * Get an argument from the tracepoint.
   *
   * @param key   the arg key
   * @param clazz the type to return as
   * @param def   the default value if none is set
   * @param <T>   the type to return as
   * @return the value from the args, or the default
   */
  public <T> T getArg(final String key, final Class<T> clazz, final T def) {
    final String s = this.args.get(key);
    if (s == null) {
      return def;
    }
    return Settings.coerc(s, clazz);
  }

  /**
   * Check if this tracepoint can fire.
   *
   * @param ts the current time in ms
   * @return {@code true} if this tracepoint can fire.
   */
  public boolean canFire(final long ts) {
    final int fireCount = this.getFireCount();

    //Have we exceeded the fire count?
    if (fireCount != -1 && fireCount <= this.stats.getFireCount()) {
      return false;
    }

    // are we inside the time window
    if (!this.window.inWindow(ts)) {
      return false;
    }

    // Have we fired too quickly?
    final long lastFire = this.stats.lastFire;
    if (lastFire != 0) {
      final long timeSinceLast = ts - lastFire;
      if (timeSinceLast < getArg(FIRE_PERIOD, Integer.class, 1000)) {
        return false;
      }
    }
    return true;
  }

  public void fired(final long ts) {
    this.stats.fired(ts);
  }

  /**
   * Does this tracepoint accept the provided stage.
   * <p>
   * We can run tracepoints at start or end of line (or method), as well as collecting at start but defer send to end. Here we
   * want to check the current visitor stage (where we are modifying the code), with the config of the tracepoint.
   * <p>
   * We should return {@code true} if the tracepoint should collect data at this stage.
   *
   * @param currentStage the stage we are trying to visit
   * @return {@code true} if this tracepoint accepts this stage, else {@code false}.
   */
  public boolean acceptStage(final EStage currentStage) {
    // if method name is set then we default to method start
    final String methodName = getArg(METHOD_NAME, String.class, null);
    final EStage targetStage;
    if (methodName != null) {
      targetStage = getArg(STAGE, EStage.class, EStage.METHOD_START);
    } else {
      targetStage = getArg(STAGE, EStage.class, EStage.LINE_START);
    }

    if (targetStage == currentStage) {
      return true;
    }

    switch (currentStage) {
      // if we are at line start - and we are line capture then capture
      case LINE_START:
      case LINE_END:
        return targetStage == EStage.LINE_CAPTURE;
      case METHOD_START:
      case METHOD_END:
        return targetStage == EStage.METHOD_CAPTURE;
      default:
        return false;
    }
  }

  private static class TracepointWindow {

    private final Integer start;
    private final Integer end;

    public TracepointWindow(final Integer start, final Integer end) {
      this.start = start;
      this.end = end;
    }

    public boolean inWindow(long ts) {
      // no window configured so we are ok
      if (this.start == 0 && this.end == 0) {
        return true;
      }

      // only end configured - return if now is less than end
      if (this.start == 0 && this.end > 0) {
        return ts <= this.end;
      }

      //only start configured - return if now is more than start
      if (this.start > 0 && this.end == 0) {
        return this.start <= ts;
      }

      // if both then check both
      return this.start <= ts && ts <= this.end;
    }
  }

  private static class TracepointExecutionStats {

    private int fireCount = 0;
    private long lastFire = 0;

    int getFireCount() {
      return this.fireCount;
    }

    void fired(final long ts) {
      this.lastFire = ts;
      this.fireCount++;
    }
  }

  /**
   * This type describes the different stages that tracepoints can trigger at.
   */
  public enum EStage {
    LINE_START(TracePointConfig.LINE_START),
    LINE_END(TracePointConfig.LINE_END),
    LINE_CAPTURE(TracePointConfig.LINE_CAPTURE),
    METHOD_START(TracePointConfig.METHOD_START),
    METHOD_END(TracePointConfig.METHOD_END),
    METHOD_CAPTURE(TracePointConfig.METHOD_CAPTURE),
    ;

    private final String name;

    EStage(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }

    /**
     * Convert a string into the equivalent {@link EStage} value.
     *
     * @param arg the string to convert
     * @return the {@link EStage} value.
     */
    public static EStage fromArg(final String arg) {
      switch (arg) {
        case TracePointConfig.LINE_END:
          return EStage.LINE_END;
        case TracePointConfig.LINE_CAPTURE:
          return EStage.LINE_CAPTURE;
        case TracePointConfig.METHOD_START:
          return EStage.METHOD_START;
        case TracePointConfig.METHOD_END:
          return EStage.METHOD_END;
        case TracePointConfig.METHOD_CAPTURE:
          return EStage.METHOD_CAPTURE;
        default:
          return EStage.LINE_START;
      }
    }
  }
}
