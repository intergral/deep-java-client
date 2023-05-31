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

package com.intergral.deep.agent.tracepoint.inst;

public class InstUtils
{

    public static String externalClassName( final String className )
    {
        return className.replaceAll( "/", "." );
    }


    public static String fileName( final String relPath )
    {
        final int lastIndexOf = relPath.lastIndexOf( '/' );
        if( lastIndexOf == -1 )
        {
            return relPath;
        }
        return relPath.substring( lastIndexOf + 1 );
    }


    public static String internalClassStripInner( final Class<?> allLoadedClass )
    {
        return internalClassStripInner( allLoadedClass.getName() );
    }


    public static String internalClassStripInner( final String allLoadedClass )
    {
        final int index = allLoadedClass.indexOf( '$' );
        if( index == -1 )
        {
            return internalClass( allLoadedClass );
        }
        return internalClass( allLoadedClass.substring( 0, index ) );
    }

    public static String internalClass( final String clazz )
    {
        return clazz.replaceAll( "\\.", "/" );
    }

    public static String internalClass( final Class<?> clazz )
    {
        return internalClass( clazz.getName() );
    }

    public static String shortClassName( final String className )
    {
        return className.substring( className.lastIndexOf( '.' ) + 1 );
    }
}
