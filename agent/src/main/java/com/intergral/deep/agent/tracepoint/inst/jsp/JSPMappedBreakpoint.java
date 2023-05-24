package com.intergral.deep.agent.tracepoint.inst.jsp;

import com.intergral.deep.agent.types.TracePointConfig;

public class JSPMappedBreakpoint extends TracePointConfig
{
    private final int mappedLine;


    public JSPMappedBreakpoint( final TracePointConfig tp, final int mappedLine )
    {
        super( tp.getId(), tp.getPath(), tp.getLineNo(), tp.getArgs(), tp.getWatches() );
        this.mappedLine = mappedLine;
    }

    @Override
    public int getLineNo()
    {
        return this.mappedLine;
    }
}
