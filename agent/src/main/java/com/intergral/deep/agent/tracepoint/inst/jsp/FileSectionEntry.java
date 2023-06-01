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

public class FileSectionEntry {

  private final int id;
  private final String sourceName;
  private final String sourcePath;


  public FileSectionEntry(final int id, final String sourceName) {
    this(id, sourceName, null);
  }


  public FileSectionEntry(final int id, final String sourceName, String sourcePath) {
    this.id = id;
    this.sourceName = sourceName;
    this.sourcePath = sourcePath;
  }


  public int getId() {
    return id;
  }


  public String getSourceName() {
    return sourceName;
  }


  public String getSourcePath() {
    return sourcePath;
  }


  @Override
  public String toString() {
    return "FileSectionEntry#" + id + ":" + sourceName + ":" + sourcePath;
  }

}
