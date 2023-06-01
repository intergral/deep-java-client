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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class SourceMapParser {

  private static final String STRATUM_SECTION = "*S";
  private static final String END_SECTION = "*E";
  private static final String FILE_SECTION = "*F";
  private static final String LINE_SECTION = "*L";

  private final String sourceMapString;
  private final int size;


  /**
   * From dsol_spec.html : Before the SMAP in a SMAP-file can be installed into the
   * SourceDebugExtension attribute it must be resolved into an SMAP with no embedded SMAPs and with
   * final-source as the output source. Which means we never need to handle embedded smaps
   *
   * @param smap source map
   */
  public SourceMapParser(final String smap) {
    this.sourceMapString = smap;
    size = smap.length();
  }


  public SourceMap parse() throws IOException {
    final BufferedReader reader = new BufferedReader(new StringReader(sourceMapString));

    final String first = reader.readLine();
    if (first == null || !first.equals("SMAP")) {
      throw new IllegalStateException("smap doesnt start with SMAP, '" + first + "'");
    }
    /*final String generateFile = */
    reader.readLine();
    final String defaultStratum = reader.readLine();

    final SourceMap topSmap = new SourceMap(defaultStratum);

    reader.mark(size);

    StratumSection current = null;
    String line = reader.readLine();
    while (line != null && !line.equals(END_SECTION)) {
      if (line.startsWith(STRATUM_SECTION)) {
        if (current != null) {
          throw new IllegalStateException();
        }

        current = readStratumSection(line);
      } else if (line.equals(FILE_SECTION)) {
        if (current == null) {
          throw new IllegalStateException();
        }
        current.setFileSection(readFileSection(reader));
      } else if (line.equals(LINE_SECTION)) {
        if (current == null) {
          throw new IllegalStateException();
        }
        current.setLineSection(readLineSection(reader));
      }
      line = reader.readLine();
    }

    // We can get corrupt smaps like noop.jsp sometimes
    if (current == null) {
      throw new IOException("Cannot find a Stratum Section in SMAP");
    }

    topSmap.addStratumSection(current);

    return topSmap;
  }


  private StratumSection readStratumSection(String currentLine) {
    final String name = currentLine.substring(2, currentLine.length());
    return new StratumSection(name.trim());
  }


  private LineSection readLineSection(BufferedReader reader) throws IOException {
    final LineSection lineSection = new LineSection();

    int fileId = 0;

    String line = reader.readLine();
    while (line != null && !line.startsWith("*")) {
      int repeatCount = 1;
      int outputLineIncrement = 1;

      final String[] lineInfos = line.split(":");

      final String inputLineInfo = lineInfos[0];
      final String outputLineInfo = lineInfos[1];
      final String[] inSplit = inputLineInfo.split(",");
      final String inputStartLine = inSplit[0];
      if (inSplit.length == 2) {
        repeatCount = Integer.parseInt(inSplit[1]);
      }
      final String[] inputStartSplit = inputStartLine.split("#");
      final int inputStart = Integer.parseInt(inputStartSplit[0]);
      if (inputStartSplit.length == 2) {
        fileId = Integer.parseInt(inputStartSplit[1]);
      }

      final String[] outSplit = outputLineInfo.split(",");
      final String outputLine = outSplit[0];
      final int outputStartLine = Integer.parseInt(outputLine);
      if (outSplit.length == 2) {
        outputLineIncrement = Integer.parseInt(outSplit[1]);
      }

      lineSection.add(new LineSectionEntry(inputStart,
          fileId,
          repeatCount,
          outputStartLine,
          outputLineIncrement));

      reader.mark(size);
      line = reader.readLine();
    }
    reader.reset();
    return lineSection;
  }


  private FileSection readFileSection(BufferedReader reader) throws IOException {
    final FileSection fileSection = new FileSection();

    String line = reader.readLine();
    while (line != null && !line.startsWith("*")) {
      if (line.startsWith("+")) {
        line = line.substring(2);

        int spaceIndex = line.indexOf(' ');

        final int id = Integer.parseInt(line.substring(0, spaceIndex));
        String sourceName = line.substring(spaceIndex + 1); //protect against only space at end

        final String sourcePath = reader.readLine();
        final FileSectionEntry entry = new FileSectionEntry(id, sourceName, sourcePath);
        fileSection.put(entry.getId(), entry);
      } else {
        int spaceIndex = line.indexOf(' ');

        final int id = Integer.parseInt(line.substring(0, spaceIndex));
        String sourceName = line.substring(spaceIndex + 1); //protect against only space at end

        final FileSectionEntry entry = new FileSectionEntry(id, sourceName);
        fileSection.put(entry.getId(), entry);
      }
      reader.mark(size);
      line = reader.readLine();
    }
    reader.reset();
    return fileSection;
  }
}
