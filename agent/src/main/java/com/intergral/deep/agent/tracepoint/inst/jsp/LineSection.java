/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class LineSection
{
    private final List<LineSectionEntry> entries = new ArrayList<LineSectionEntry>();


    public void add( final LineSectionEntry entry )
    {
        entries.add( entry );
    }


    public Iterator<LineSectionEntry> iterator()
    {
        return entries.iterator();
    }

}
