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

package com.intergral.deep.agent.tracepoint.inst.jsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SourceMap {

  private final String defaultStratum;
  private final Map<String, StratumSection> nameToStratrumSectionMap = new HashMap<>();


  public SourceMap(final String defaultStratum) {
    this.defaultStratum = defaultStratum;
  }


  public void addStratumSection(final StratumSection section) {
    nameToStratrumSectionMap.put(section.getName(), section);
  }


  /**
   * Maps the filename and line number to line numbers in the output file.
   *
   * @param filename   the stratum filename (i.e fib.jsp)
   * @param lineNumber the line number
   * @return the list of line numbers in the output file (i.e. fib_jsp.java file)
   */
  public List<SourceMapLineStartEnd> map(final String filename, final long lineNumber) {
    // map fib.jsp:11 to line number in this class.
    final StratumSection stratum = nameToStratrumSectionMap.get(defaultStratum);

    final List<SourceMapLineStartEnd> javaLineNumbers = new ArrayList<SourceMapLineStartEnd>();

    final Iterator<LineSectionEntry> it = stratum.getLineSection().iterator();
    while (it.hasNext()) {
      final LineSectionEntry entry = it.next();
      final FileSectionEntry file = stratum.getFileSection().get(entry.getLineFileID());
      if (file.getSourceName().equals(filename)) {
        SourceMapLineStartEnd startEnd = null;

        int o = entry.getOutputStartLine();

        for (int i = entry.getInputStartLine();
            i < entry.getInputStartLine() + entry.getRepeatCount(); i++) {
          if (entry.getOutputLineIncrement() == 0) {
            // Not sure if ever happens but I saw a bug which says 0 increment isnt against the spec
            if (i == lineNumber) {
              startEnd = new SourceMapLineStartEnd(o);
            } else if (startEnd != null) {
              javaLineNumbers.add(startEnd);
              startEnd = null;
            }
          } else {
            for (int c = 0; c < entry.getOutputLineIncrement(); c++) {
              if (i == lineNumber) {
                if (startEnd == null) {
                  startEnd = new SourceMapLineStartEnd(o);
                } else {
                  startEnd.setEnd(o);
                }
              } else if (startEnd != null) {
                javaLineNumbers.add(startEnd);
                startEnd = null;
              }
              o++;
            }
          }
        }
        if (startEnd != null) {
          javaLineNumbers.add(startEnd);
        }
      }
    }
    return javaLineNumbers;
  }


  /**
   * Look a linenumber from a output file to a filename and linenumner
   *
   * @param lineNumber the linenumber to look for (i.e. include_005ftime_jsp.java:91)
   * @return the filename and linenumber it maps to (i.e. time.jsp:1)
   */
  public SourceMapLookup lookup(final int lineNumber) {
    // lookup include_005ftime_jsp.java:91 which should match to time.jsp:1
    final StratumSection stratum = nameToStratrumSectionMap.get(defaultStratum);

    final Iterator<LineSectionEntry> it = stratum.getLineSection().iterator();
    while (it.hasNext()) {
      final LineSectionEntry entry = it.next();
      final FileSectionEntry file = stratum.getFileSection().get(entry.getLineFileID());

      int o = entry.getOutputStartLine();

      for (int i = entry.getInputStartLine();
          i < entry.getInputStartLine() + entry.getRepeatCount(); i++) {
        if (o == lineNumber) {
          return new SourceMapLookup(file.getSourceName(), i);
        }
        //                if( entry.getOutputLineIncrement() == 0 )
        //                {
        //                    // Not sure if ever happens but I saw a bug which says 0 increment isnt against the spec
        //                    if( i == lineNumber )
        //                    {
        //
        //                    }
        //                }
        //                else
        //                {
        for (int c = 0; c < entry.getOutputLineIncrement(); c++) {
          if (o == lineNumber) {
            return new SourceMapLookup(file.getSourceName(), i);
          }
          o++;
        }
        //                }
      }
    }
    return null;
  }


  public List<String> getFilenames() {
    final StratumSection stratum = nameToStratrumSectionMap.get(defaultStratum);
    return stratum.getFileSection().getFilenames();
  }

}
