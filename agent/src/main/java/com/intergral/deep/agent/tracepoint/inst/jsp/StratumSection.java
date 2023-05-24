/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class StratumSection
{
    private final String name;
    private FileSection fileSection;
    private LineSection lineSection;


    public StratumSection( final String name )
    {
        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    public void setFileSection( final FileSection fileSection )
    {
        this.fileSection = fileSection;
    }


    public FileSection getFileSection()
    {
        return fileSection;
    }


    public void setLineSection( final LineSection lineSection )
    {
        this.lineSection = lineSection;
    }


    public LineSection getLineSection()
    {
        return lineSection;
    }
}
