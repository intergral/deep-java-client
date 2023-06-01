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

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.plugin.IEventContext;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class FrameProcessor extends FrameCollector implements IEventContext {

  private final Collection<TracePointConfig> tracePointConfigs;
  private final long[] lineStart;
  private Collection<TracePointConfig> filteredTracepoints;
  private TracePointConfig currentTracePointConfig;

  public FrameProcessor(final Settings settings,
      final IEvaluator evaluator,
      final Map<String, Object> variables,
      final Collection<TracePointConfig> tracePointConfigs,
      final long[] lineStart, final StackTraceElement[] stack) {
    super(settings, evaluator, variables, stack);
    this.tracePointConfigs = tracePointConfigs;
    this.lineStart = lineStart;
  }

  public boolean canCollect() {
    this.filteredTracepoints = this.tracePointConfigs.stream()
        .filter(tracePointConfig -> tracePointConfig.canFire(this.lineStart[0])
            && this.conditionPasses(tracePointConfig))
        .collect(Collectors.toList());

    return this.filteredTracepoints.size() != 0;
  }

  private boolean conditionPasses(final TracePointConfig tracePointConfig) {
    final String condition = tracePointConfig.getCondition();
    if (condition == null || condition.trim().isEmpty()) {
      return true;
    }

    return this.evaluator.evaluate(condition, variables);
  }

  public void configureSelf() {
    for (TracePointConfig tracePointConfig : this.filteredTracepoints) {
      this.frameConfig.process(tracePointConfig);
    }
    this.frameConfig.close();
  }

  public Collection<EventSnapshot> collect() {
    final Collection<EventSnapshot> snapshots = new ArrayList<>();

    final IFrameResult processedFrame = super.processFrame();

    for (final TracePointConfig tracepoint : filteredTracepoints) {
      try (final AutoCloseable ignored = withTracepoint(tracepoint)) {
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

  private AutoCloseable withTracepoint(final TracePointConfig tracepoint) {
    this.currentTracePointConfig = tracepoint;
    return () -> currentTracePointConfig = null;
  }

  protected <T> T getTracepointConfig(final String key, final Class<T> clazz, T def) {
    if (currentTracePointConfig == null) {
      return def;
    }
    return currentTracePointConfig.getArg(key, clazz, def);
  }

  @Override
  public String evaluateExpression(final String expression) throws Throwable {
    final Object o = this.evaluator.evaluateExpression(expression, this.variables);
    return Utils.valueOf(o);
  }

  public interface IFactory {

    FrameProcessor provide(Settings settings, IEvaluator evaluator, Map<String, Object> variables,
        Collection<TracePointConfig> tracePointConfigs, long[] lineStart,
        StackTraceElement[] stack);
  }
}
