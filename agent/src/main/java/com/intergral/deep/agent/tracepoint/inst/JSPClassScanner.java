package com.intergral.deep.agent.tracepoint.inst;


import com.intergral.deep.agent.tracepoint.inst.jsp.JSPUtils;
import com.intergral.deep.agent.types.TracePointConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService.loadJspBreakpoints;


public class JSPClassScanner implements IClassScanner
{
    private final Map<String, TracePointConfig> removedBreakpoints;
    private final String jspSuffix;
    private final List<String> jspPackages;


    public JSPClassScanner( final Map<String, TracePointConfig> removedBreakpoints,
                            final String jspSuffix,
                            final List<String> jspPackages )
    {
        this.removedBreakpoints = removedBreakpoints;
        this.jspSuffix = jspSuffix;
        this.jspPackages = jspPackages;
    }


    @Override
    public boolean scanClass( final Class<?> loadedClass )
    {
        if( removedBreakpoints.isEmpty() )
        {
            // stop as soon as we run out of classes
            return false;
        }
        if( JSPUtils.isJspClass( this.jspSuffix, this.jspPackages, loadedClass.getName() ) )
        {
            final Set<TracePointConfig> breakpoints = loadJspBreakpoints( loadedClass, removedBreakpoints );
            if( !breakpoints.isEmpty() )
            {
                for( TracePointConfig breakpoint : breakpoints )
                {
                    removedBreakpoints.remove( breakpoint.getId() );
                }
                return true;
            }
        }
        return false;
    }
}
