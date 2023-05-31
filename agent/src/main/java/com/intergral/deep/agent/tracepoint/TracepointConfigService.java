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

package com.intergral.deep.agent.tracepoint;

import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
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
                .collect( Collectors.toList() );
    }
}
