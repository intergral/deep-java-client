/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class SourceMapLineStartEnd
{
    private final int start;
    private int end;


    public SourceMapLineStartEnd( final int start )
    {
        this.start = start;
        this.end = start;
    }


    public void setEnd( int end )
    {
        this.end = end;
    }


    public int getStart()
    {
        return start;
    }


    public int getEnd()
    {
        return end;
    }
}
