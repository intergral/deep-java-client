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
            return TracepointInstrumentationService.loadCfBreakpoints( CFUtils.guessSource( loadedClass.getName() ), values );
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
