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

import com.intergral.deep.agent.settings.Settings;
import java.util.Collection;
import java.util.Map;

public class TracePointConfig {

  /**
   * Collect only the frame we are on
   */
  public static final String SINGLE_FRAME_TYPE = "single_frame";
  /**
   * Collect from all available frames
   */
  public static final String ALL_FRAME_TYPE = "all_frame";
  /**
   * Collect on frame data
   */
  public static final String NO_FRAME_TYPE = "no_frame";
  /**
   * Collect the full stack
   */
  public static final String STACK = "stack";
  /**
   * The number of times this tracepoint should fire
   */
  private static final String FIRE_COUNT = "fire_count";
  /**
   * The start of the time period this tracepoint can fire in
   */
  private static final String WINDOW_START = "window_start";
  /**
   * The end of the time period this tracepoint can fire in
   */
  private static final String WINDOW_END = "window_end";
  /**
   * The minimum time between successive triggers, in ms
   */
  private static final String FIRE_PERIOD = "fire_period";
  /**
   * The condition that has to be 'truthy' for this tracepoint to fire
   */
  public static final String CONDITION = "condition";
  /**
   * This is the key to indicate the frame collection type
   */
  private static final String FRAME_TYPE = "frame_type";
  /**
   * This is the key to indicate the stack collection type
   */
  private static final String STACK_TYPE = "stack_type";

  private final String id;
  private final String path;
  private final int lineNo;
  private final Map<String, String> args;
  private final Collection<String> watches;
  private final TracepointWindow window;

  private final TracepointExecutionStats stats = new TracepointExecutionStats();


  public TracePointConfig(final String id, final String path, final int lineNo,
      final Map<String, String> args,
      final Collection<String> watches) {
    this.id = id;
    this.path = path;
    this.lineNo = lineNo;
    this.args = args;
    this.watches = watches;
    this.window = new TracepointWindow(this.getArg(WINDOW_START, Integer.class, 0),
        this.getArg(WINDOW_END, Integer.class, 0));
  }

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

  public String getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Map<String, String> getArgs() {
    return args;
  }

  public Collection<String> getWatches() {
    return watches;
  }

  public int getFireCount() {
    return getArg(FIRE_COUNT, Integer.class, 1);
  }

  public String getCondition() {
    return getArg(CONDITION, String.class, null);
  }

  public String getFrameType() {
    return getArg(FRAME_TYPE, String.class, SINGLE_FRAME_TYPE);
  }

  public String getStackType() {
    return getArg(STACK_TYPE, String.class, STACK);
  }

  public <T> T getArg(final String key, final Class<T> clazz, final T def) {
    final String s = this.args.get(key);
    if (s == null) {
      return def;
    }
    return Settings.coerc(s, clazz);
  }

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
}
