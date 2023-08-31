package com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileSectionEntryTest
{


    private FileSectionEntry fileSectionEntry;


    @BeforeEach
    void setUp()
    {
        new FileSectionEntry( 10, "source" );
        fileSectionEntry = new FileSectionEntry( 10, "source", "path" );
    }


    @Test
    void getSourceName()
    {
        assertEquals( "source", fileSectionEntry.getSourceName() );
    }


    @Test
    void getId()
    {
        assertEquals( 10, fileSectionEntry.getId() );
    }


    @Test
    void getSourcePath()
    {
        assertEquals( "path", fileSectionEntry.getSourcePath() );
    }


    @Test
    void toStringTest()
    {
        assertEquals( "FileSectionEntry#10:source:path", fileSectionEntry.toString() );
    }
}
