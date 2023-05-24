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

package com.intergral.deep.agent.tracepoint;

import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class TracepointConfigService implements ITracepointConfig
{
    private final static Logger LOGGER = LoggerFactory.getLogger( TracepointConfigService.class );
    private final TracepointInstrumentationService tracepointInstrumentationService;
    private String currentHash = null;
    private Collection<TracePointConfig> installedTracepoints;
    private long lastUpdate;

    public TracepointConfigService( final TracepointInstrumentationService tracepointInstrumentationService )
    {
        this.tracepointInstrumentationService = tracepointInstrumentationService;
    }

    @Override
    public void noChange( long tsNano )
    {
        LOGGER.debug( "No change to tracepoint config." );
        this.lastUpdate = tsNano;
    }

    @Override
    public void configUpdate( long tsNano, String hash, Collection<TracePointConfig> tracepoints )
    {
        this.currentHash = hash;
        this.installedTracepoints = tracepoints;
        this.lastUpdate = tsNano;
        this.tracepointInstrumentationService.processBreakpoints( tracepoints );
    }

    @Override
    public String currentHash()
    {
        return this.currentHash;
    }

    @Override
    public Collection<TracePointConfig> loadTracepointConfigs( final Collection<String> tracepointId )
    {
        return installedTracepoints.stream()
                .filter( tracePointConfig -> tracepointId.contains( tracePointConfig.getId() ) )
                .collect( Collectors.toList(  ) );
    }
}
