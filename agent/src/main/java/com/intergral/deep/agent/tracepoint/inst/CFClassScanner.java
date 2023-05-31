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

package com.intergral.deep.agent.tracepoint.inst;

import com.intergral.deep.agent.tracepoint.cf.CFUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

public class CFClassScanner implements IClassScanner
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CFClassScanner.class );
    private final Map<String, TracePointConfig> removedBreakpoints;


    public CFClassScanner( final Map<String, TracePointConfig> removedBreakpoints )
    {
        this.removedBreakpoints = removedBreakpoints;
    }


    @Override
    public boolean scanClass( final Class<?> loadedClass )
    {
        if( removedBreakpoints.isEmpty() )
        {
            // stop as soon as we run out of classes
            return false;
        }

        if( CFUtils.isCfClass( loadedClass.getName() ) )
        {
            try
            {
                final Set<TracePointConfig> breakpoints = loadCfBreakpoints( loadedClass, removedBreakpoints );
                if( !breakpoints.isEmpty() )
                {
                    for( TracePointConfig breakpoint : breakpoints )
                    {
                        removedBreakpoints.remove( breakpoint.getId() );
                    }
                    return true;
                }
            }
            catch( Exception e )
            {
                LOGGER.error( "Error processing class {}", loadedClass, e );
            }
        }

        return false;
    }


    private Set<TracePointConfig> loadCfBreakpoints( final Class<?> loadedClass,
                                                     final Map<String, TracePointConfig> values )
    {
        final URL location = getLocation( loadedClass );
        if( location == null )
        {
            return TracepointInstrumentationService.loadCfBreakpoints( CFUtils.guessSource( loadedClass.getName() ),
                    values );
        }
        return TracepointInstrumentationService.loadCfBreakpoints( location, values );
    }


    URL getLocation( final Class<?> loadedClass )
    {
        return getLocation( loadedClass.getProtectionDomain() );
    }


    URL getLocation( final ProtectionDomain protectionDomain )
    {
        return protectionDomain.getCodeSource().getLocation();
    }
}
