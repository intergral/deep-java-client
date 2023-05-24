/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.evaluator.IEvaluator;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class FrameProcessor extends FrameCollector
{
    private final Collection<TracePointConfig> tracePointConfigs;
    private final long lineStart;
    private Collection<TracePointConfig> filteredTracepoints;

    public FrameProcessor( final Settings settings,
                           final IEvaluator evaluator,
                           final Map<String, Object> variables,
                           final Collection<TracePointConfig> tracePointConfigs,
                           final long lineStart, final StackTraceElement[] stack )
    {
        super( settings, evaluator, variables, stack );
        this.tracePointConfigs = tracePointConfigs;
        this.lineStart = lineStart;
    }

    public boolean canCollect()
    {
        this.filteredTracepoints = this.tracePointConfigs.stream()
                .filter( tracePointConfig -> tracePointConfig.canFire( this.lineStart ) &&
                        this.conditionPasses( tracePointConfig ) )
                .collect( Collectors.toList() );

        return this.filteredTracepoints.size() != 0;
    }

    private boolean conditionPasses( final TracePointConfig tracePointConfig )
    {
        final String condition = tracePointConfig.getCondition();
        if( condition == null || condition.trim().isEmpty() )
        {
            return true;
        }

        return this.evaluator.evaluate( condition, variables );
    }

    public void configureSelf()
    {
        for( TracePointConfig tracePointConfig : this.filteredTracepoints )
        {
            this.frameConfig.process( tracePointConfig );
        }
        this.frameConfig.close();
    }

    public Collection<EventSnapshot> collect()
    {
        final Collection<EventSnapshot> snapshots = new ArrayList<>();

        final IFrameResult processedFrame = super.processFrame();

        for( final TracePointConfig tracepoint : filteredTracepoints )
        {
            final EventSnapshot snapshot = new EventSnapshot( tracepoint,
                    this.lineStart,
                    this.settings.getResource(),
                    processedFrame.frames(),
                    processedFrame.variables() );

            for( String watch : tracepoint.getWatches() )
            {
                final FrameCollector.IExpressionResult result = super.evaluateExpression( watch );
                snapshot.addWatchResult( result.result(), result.variables() );
            }

            final Resource attributes = super.processAttributes( tracepoint );
            snapshot.mergeAttributes( attributes );

            snapshots.add( snapshot );
            tracepoint.fired( this.lineStart );
        }

        return snapshots;
    }
}
