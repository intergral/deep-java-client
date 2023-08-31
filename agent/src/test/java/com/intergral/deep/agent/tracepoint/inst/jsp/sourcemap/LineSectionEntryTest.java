package com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LineSectionEntryTest
{


    private LineSectionEntry lineSectionEntry;


    @BeforeEach
    void setUp()
    {
        lineSectionEntry = new LineSectionEntry( 1, 101, 10, 202, 2 );
    }


    @Test
    void testToString()
    {
        assertEquals( "LineSectionEntry#101 1 10 202 2", lineSectionEntry.toString() );
    }
}
