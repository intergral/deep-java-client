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

package com.intergral.deep.agent.types.snapshot;

import com.intergral.deep.agent.IDUtils;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.types.TracePointConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventSnapshot
{
    private final String id;
    private final TracePointConfig tracepoint;
    private final Map<String, Variable> varLookup;
    private final long nanoTs;
    private final Collection<StackFrame> frames;
    private final ArrayList<WatchResult> watches;
    private final long durationNanos;
    private final Resource resource;
    private Resource attributes;
    private boolean open;

    public EventSnapshot( final TracePointConfig tracepoint,
                          final long nanoTs,
                          final Resource resource,
                          final Collection<StackFrame> frames,
                          final Map<String, Variable> variables )
    {
        this.id = IDUtils.randomId();
        this.tracepoint = tracepoint;
        this.varLookup = new HashMap<>( variables );
        this.nanoTs = nanoTs;
        this.frames = frames;
        this.watches = new ArrayList<>();
        this.attributes = Resource.create( Collections.emptyMap() );
        this.durationNanos = 0;
        this.resource = Resource.create( resource.getAttributes(), resource.getSchemaUrl() );
        this.open = true;
    }

    public void addWatchResult( final WatchResult result, final Map<String, Variable> variables )
    {
        if( this.open )
        {
            this.watches.add( result );
            this.varLookup.putAll( variables );
        }
    }

    public void mergeAttributes( final Resource attributes )
    {
        if( this.open )
        {
            this.attributes = this.attributes.merge( attributes );
        }
    }

    public String getID()
    {
        return id;
    }

    public TracePointConfig getTracepoint()
    {
        return tracepoint;
    }

    public Map<String, Variable> getVarLookup()
    {
        return varLookup;
    }

    public long getNanoTs()
    {
        return nanoTs;
    }

    public Collection<StackFrame> getFrames()
    {
        return frames;
    }

    public ArrayList<WatchResult> getWatches()
    {
        return watches;
    }

    public long getDurationNanos()
    {
        return durationNanos;
    }

    public Resource getResource()
    {
        return resource;
    }

    public Resource getAttributes()
    {
        return attributes;
    }
}
