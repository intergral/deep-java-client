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

public class StratumSection {

  private final String name;
  private FileSection fileSection;
  private LineSection lineSection;


  public StratumSection(final String name) {
    this.name = name;
  }


  public String getName() {
    return name;
  }


  public void setFileSection(final FileSection fileSection) {
    this.fileSection = fileSection;
  }


  public FileSection getFileSection() {
    return fileSection;
  }


  public void setLineSection(final LineSection lineSection) {
    this.lineSection = lineSection;
  }


  public LineSection getLineSection() {
    return lineSection;
  }
}
