/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
