/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SourceMapParserTest {

  public SourceMapParserTest() {
  }


  private String fileToString(final String filename) throws IOException {
    final byte[] bytes;
    try (InputStream resourceAsStream = getClass().getResourceAsStream(filename)) {
      bytes = new byte[resourceAsStream.available()];
      resourceAsStream.read(bytes);
    }
    return new String(bytes);
  }


  @Test
  public void testIndexJsp() throws Exception {
    final SourceMapParser parser = new SourceMapParser(fileToString("/index_jsp.smap"));
    final SourceMap smap = parser.parse();

    //smap.dumpInformation();
    {
      final List<SourceMapLineStartEnd> found = smap.map("header.jsp", 1);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(71, entry.getStart(), "Should start @ line 71");
      assertEquals(71, entry.getEnd(), "Should end @ line 71");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("header.jsp", 11);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(83, entry.getStart(), "Should start @ line 83");
      assertEquals(87, entry.getEnd(), "Should end @ line 87");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("footer.jsp", 4);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(138, entry.getStart(), "Should start @ line 138");
      assertEquals(140, entry.getEnd(), "Should end @ line 140");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("index.jsp", 4);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(92, entry.getStart(), "Should start @ line 92");
      assertEquals(92, entry.getEnd(), "Should end @ line 92");
    }
  }


  /* Test based on example table from JSR-045 document
   Input Source Output Source
   Line  Begin Line  End Line
   123  207  207
   130  210  210
   131  211  211
   132  212  212
   140  250  256
   160  300  301
   161  302  303
   162  304  305*/
  @Test
  public void testIndex2Jsp() throws Exception {
    final SourceMapParser parser = new SourceMapParser(fileToString("/index2_jsp.smap"));
    final SourceMap smap = parser.parse();

    //        smap.dumpInformation();
    {
      final List<SourceMapLineStartEnd> found = smap.map("index2.jsp", 123);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(207, entry.getStart(), "Should start @ line 207");
      assertEquals(207, entry.getEnd(), "Should end @ line 207");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("index2.jsp", 140);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(250, entry.getStart(), "Should start @ line 250");
      assertEquals(256, entry.getEnd(), "Should end @ line 256");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("index2.jsp", 160);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(300, entry.getStart(), "Should start @ line 300");
      assertEquals(301, entry.getEnd(), "Should end @ line 301");
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("index2.jsp", 162);
      assertEquals(1, found.size(), "Should fine 1 line");

      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(304, entry.getStart(), "Should start @ line 304");
      assertEquals(305, entry.getEnd(), "Should end @ line 305");
    }
  }


  @Test
  public void testParseIncludeTimeJsp() throws Exception {
    // Test with a page including the same jsp twice.

    final SourceMapParser parser = new SourceMapParser(fileToString("/include_time.smap"));
    final SourceMap smap = parser.parse();

    //        smap.dumpInformation();
    {
      final List<SourceMapLineStartEnd> found = smap.map("time.jsp", 1);
      assertEquals(2, found.size(), "Should fine 2 line");
      {
        final SourceMapLineStartEnd entry = found.get(0);
        assertEquals(90, entry.getStart(), "Should start @ line 90");
        assertEquals(91, entry.getEnd(), "Should end @ line 91");
      }
      {
        final SourceMapLineStartEnd entry = found.get(1);
        assertEquals(99, entry.getStart(), "Should start @ line 99");
        assertEquals(100, entry.getEnd(), "Should end @ line 100");
      }
    }

    {
      final List<SourceMapLineStartEnd> found = smap.map("footer.jsp", 29);
      assertEquals(1, found.size(), "Should fine 1 line");
      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(140, entry.getStart(), "Should start @ line 140");
      assertEquals(140, entry.getEnd(), "Should end @ line 140");
    }
  }


  @Test
  public void testParseIncludeTime2Jsp() throws Exception {
    final SourceMapParser parser = new SourceMapParser(fileToString("/include_time2.smap"));
    final SourceMap smap = parser.parse();

    //smap.dumpInformation();

    final List<SourceMapLineStartEnd> found = smap.map("time.jsp", 1);
    assertEquals(4, found.size(), "Should contain 4 entries");
    {
      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(91, entry.getStart(), "Should start @ line 91");
      assertEquals(92, entry.getEnd(), "Should end @ line 92");
    }
    {
      final SourceMapLineStartEnd entry = found.get(1);
      assertEquals(102, entry.getStart(), "Should start @ line 102");
      assertEquals(103, entry.getEnd(), "Should end @ line 103");
    }
    {
      final SourceMapLineStartEnd entry = found.get(2);
      assertEquals(114, entry.getStart(), "Should start @ line 114");
      assertEquals(115, entry.getEnd(), "Should end @ line 115");
    }
    {
      final SourceMapLineStartEnd entry = found.get(3);
      assertEquals(117, entry.getStart(), "Should start @ line 117");
      assertEquals(118, entry.getEnd(), "Should end @ line 119");
    }
  }


  @Test
  public void testIncludeTime3Jsp() throws Exception {
    final SourceMapParser parser = new SourceMapParser(fileToString("/include_time3.smap"));
    final SourceMap smap = parser.parse();

    final List<SourceMapLineStartEnd> found = smap.map("time.jsp", 1);
    assertEquals(3, found.size(), "Should contain 3 entries");
    {
      final SourceMapLineStartEnd entry = found.get(0);
      assertEquals(91, entry.getStart(), "Should start @ line 91");
      assertEquals(92, entry.getEnd(), "Should end @ line 92");
    }
    {
      final SourceMapLineStartEnd entry = found.get(1);
      assertEquals(102, entry.getStart(), "Should start @ line 102");
      assertEquals(103, entry.getEnd(), "Should end @ line 103");
    }
    {
      final SourceMapLineStartEnd entry = found.get(2);
      assertEquals(113, entry.getStart(), "Should start @ line 113");
      assertEquals(116, entry.getEnd(), "Should end @ line 116");
    }
  }


  @Test
  public void testIncludeTime3JspLookup() throws Exception {
    final String s = fileToString("/include_time3.smap");
    final SourceMapParser parser = new SourceMapParser(s);
    final SourceMap smap = parser.parse();

    //smap.dumpInformation();
    // line 91 doesnt map back to line 1 as there 2 files which match to line 91
    // and the first include_time.jsp should take precident according to the spec.
    // 1 include_time.jsp 4 -> 91
    // 2 time.jsp 1 -> 91
    final SourceMapLookup lookup = smap.lookup(92);

    assertEquals("time.jsp", lookup.getFilename(), "Should find time.jsp line 1");
    assertEquals(1, lookup.getLineNumber(), "Should find time.jsp line 1");
  }


  @Test
  public void testCompaniesJsp() throws Exception {
    final URL resourceUrl = getClass().getResource("/org/apache/jsp/jdbc/views/companies_jsp.class");
    final URL[] classUrls = {resourceUrl};
    final URLClassLoader ucl = new URLClassLoader(classUrls);
    final Class c = ucl.loadClass("org.apache.jsp.jdbc.views.companies_jsp");

    final String sourceDebugExtension = SmapUtils.lookUp(c);
    assertNotNull(sourceDebugExtension, "smap should not be null");
    assertFalse(sourceDebugExtension.isEmpty(), "smap should not be emtpy");

    assertTrue(sourceDebugExtension.startsWith("SMAP"), "smap should start with SMAP");
    assertTrue(sourceDebugExtension.trim().endsWith("*E"), "smap should end with *E");

    final SourceMapParser parser = new SourceMapParser(sourceDebugExtension);
    final SourceMap smap = parser.parse();

    //smap.dumpInformation();

    final SourceMapLookup lookup = smap.lookup(458);
    assertNull(lookup, "Should not find a line for 458");

    assertEquals(71, smap.lookup(457).getLineNumber());
  }


  @Test
  public void testNoopJspBug() throws Exception {
    // This code will cause a Null pointer exception FR-5116
    // as the SMAP is corrupt
    final String s = fileToString("/noop.smap");
    final SourceMapParser parser = new SourceMapParser(s);
    try {
      final SourceMap smap = parser.parse();
      fail("Should not parse");
    } catch (IOException ioe) {
      assertEquals("Cannot find a Stratum Section in SMAP", ioe.getMessage());
    }
  }
}
