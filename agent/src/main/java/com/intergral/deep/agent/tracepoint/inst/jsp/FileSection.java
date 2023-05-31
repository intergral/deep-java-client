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
import java.util.List;

public class FileSection
{
    private final HashMap<Integer, FileSectionEntry> entries = new HashMap<>();


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
        final List<String> filenames = new ArrayList<>();
        for( final FileSectionEntry entry : entries.values() )
        {
            filenames.add( entry.getSourceName() );
        }
        return filenames;
    }
}
