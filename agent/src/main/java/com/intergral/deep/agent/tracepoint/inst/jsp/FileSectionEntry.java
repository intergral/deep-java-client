/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class FileSectionEntry
{
    private final int id;
    private final String sourceName;
    private String sourcePath;


    public FileSectionEntry( final int id, final String sourceName )
    {
        this( id, sourceName, null );
    }


    public FileSectionEntry( final int id, final String sourceName, String sourcePath )
    {
        this.id = id;
        this.sourceName = sourceName;
        this.sourcePath = sourcePath;
    }


    public int getId()
    {
        return id;
    }


    public String getSourceName()
    {
        return sourceName;
    }


    public String getSourcePath()
    {
        return sourcePath;
    }


    @Override
    public String toString()
    {
        return "FileSectionEntry#" + id + ":" + sourceName + ":" + sourcePath;
    }

}
