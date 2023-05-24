/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileSection
{
    private final HashMap<Integer, FileSectionEntry> entries = new HashMap<Integer, FileSectionEntry>();


    public void put( final int id, final FileSectionEntry entry )
    {
        entries.put( id, entry );
    }


    public FileSectionEntry get( final int id )
    {
        return entries.get( id );
    }


    public List<String> getFilenames()
    {
        final List<String> filenames = new ArrayList<String>();
        for( final FileSectionEntry entry : entries.values() )
        {
            filenames.add( entry.getSourceName() );
        }
        return filenames;
    }
}
