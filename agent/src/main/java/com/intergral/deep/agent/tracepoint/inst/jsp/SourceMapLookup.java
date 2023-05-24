/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class SourceMapLookup
{

    private final String filename;
    private final int lineNumber;


    public SourceMapLookup( final String filename, final int lineNumber )
    {
        this.filename = filename;
        this.lineNumber = lineNumber;
    }


    public String getFilename()
    {
        return filename;
    }


    public int getLineNumber()
    {
        return lineNumber;
    }
}
