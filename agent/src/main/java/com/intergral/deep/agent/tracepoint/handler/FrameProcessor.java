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

package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.Reflection;
import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.plugin.EvaluationException;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This type deals with matching tracepoints to the current state and working out if we can collect the data.
 */
public class FrameProcessor extends FrameCollector implements ISnapshotContext {

  /**
   * The tracepoints that have been triggered by the Callback.
   */
  private final Collection<TracePointConfig> tracePointConfigs;
  /**
   * The time as a tuple when this line Callback started.
   *
   * @see Utils#currentTimeNanos()
   */
  private final long[] lineStart;

  /**
   * These are the tracepoints that are filtered based on state and conditions to be valid to trigger collection.
   */
  private Collection<TracePointConfig> filteredTracepoints;

  /**
   * Create a new processor for this Callback.
   *
   * @param settings          the current settings being used
   * @param evaluator         the evaluator to use for watchers and conditions
   * @param variables         the variables we have at this state
   * @param tracePointConfigs the tracepoints that are part of this Callback
   * @param lineStart         the Tuple of the time this Callback started
   * @param stack             the stack trace to use
   */
  public FrameProcessor(final Settings settings,
      final IEvaluator evaluator,
      final Map<String, Object> variables,
      final Collection<TracePointConfig> tracePointConfigs,
      final long[] lineStart, final StackTraceElement[] stack) {
    super(settings, evaluator, variables, stack);
    this.tracePointConfigs = tracePointConfigs;
    this.lineStart = lineStart;
  }

  /**
   * Using the {@link #tracePointConfigs} can we collect any data at this point.
   * <p>
   * This method will check the tracepoint config fire count, rate limits, windows and conditions and populate the
   * {@link #filteredTracepoints}
   *
   * @return {@code true}, if the {@link #filteredTracepoints} have any values
   */
  public boolean canCollect() {
    this.filteredTracepoints = this.tracePointConfigs.stream()
        .filter(tracePointConfig -> tracePointConfig.canFire(this.lineStart[0])
            && this.conditionPasses(tracePointConfig))
        .collect(Collectors.toList());

    return !this.filteredTracepoints.isEmpty();
  }

  /**
   * Process the tracepoints condition with the evaluator to see if the condition is {@code true}.
   *
   * @param tracePointConfig the config to check
   * @return {@code false} if the condition on the tracepoint evaluates to a false
   * @see IEvaluator#evaluate(String, Map)
   */
  private boolean conditionPasses(final TracePointConfig tracePointConfig) {
    final String condition = tracePointConfig.getCondition();
    if (condition == null || condition.trim().isEmpty()) {
      return true;
    }

    return this.evaluator.evaluate(condition, variables);
  }

  /**
   * Using the {@link #filteredTracepoints} update the config to reflect the collection config for this Callback.
   * <p>
   * If there are multiple tracepoints being process, the config will reflect the most inclusive, ie the higher number.
   */
  public void configureSelf() {
    configureSelf(this.filteredTracepoints);
  }

  /**
   * Collect the data into {@link EventSnapshot}.
   *
   * @return the collected {@link EventSnapshot}
   */
  public Collection<EventSnapshot> collect() {
    final Collection<EventSnapshot> snapshots = new ArrayList<>();

    final IFrameResult processedFrame = super.processFrame();

    for (final TracePointConfig tracepoint : filteredTracepoints) {
      try {
        final EventSnapshot snapshot = new EventSnapshot(tracepoint,
            this.lineStart[1],
            this.settings.getResource(),
            processedFrame.frames(),
            processedFrame.variables());

        for (String watch : tracepoint.getWatches()) {
          final FrameCollector.IExpressionResult result = super.evaluateWatchExpression(watch);
          snapshot.addWatchResult(result.result(), result.variables());
        }

        final Resource attributes = super.processAttributes(tracepoint);
        snapshot.mergeAttributes(attributes);

        snapshots.add(snapshot);
        tracepoint.fired(this.lineStart[0]);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return snapshots;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String evaluateExpression(final String expression) throws EvaluationException {
    try {
      final Object o = this.evaluator.evaluateExpression(expression, this.variables);
      return Utils.valueOf(o);
    } catch (Throwable t) {
      throw new EvaluationException(expression, t);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IReflection reflectionService() {
    return Reflection.getInstance();
  }

  /**
   * This defines a functional interface to allow for creating difference processors in the Callback.
   */
  public interface IFactory {

    /**
     * Create a new processor.
     *
     * @param settings          the current settings being used
     * @param evaluator         the evaluator to use for watchers and conditions
     * @param variables         the variables we have at this state
     * @param tracePointConfigs the tracepoints that are part of this Callback
     * @param lineStart         the Tuple of the time this Callback started
     * @param stack             the stack trace to use
     * @return the new {@link FrameProcessor}
     * @see FrameProcessor
     * @see com.intergral.deep.agent.tracepoint.cf.CFFrameProcessor
     */
    FrameProcessor provide(Settings settings, IEvaluator evaluator, Map<String, Object> variables,
        Collection<TracePointConfig> tracePointConfigs, long[] lineStart,
        StackTraceElement[] stack);
  }
}
