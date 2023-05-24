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

package com.intergral.deep.agent.tracepoint;

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.types.TracePointConfig;

public class TracepointUtils
{
    public static String estimatedClassRoot( final TracePointConfig tp )
    {
        // the arg class_name is sent from ATP as it has the full class name already
        final String className = tp.getArg( "class_name", String.class, null );
        if( className != null )
        {
            return InstUtils.internalClass( className );
        }

        return asFullClassName( tp );
    }


    private static String asFullClassName( TracePointConfig tp )
    {
        return parseFullClassName( tp.getPath(), tp.getArg( "src_root", String.class, null ) );
    }


    static String parseFullClassName( final String rawRelPath, final String srcRootArg )
    {
        if( rawRelPath.endsWith( ".cfm" ) || rawRelPath.endsWith( ".cfc" ) )
        {
            return "cfm";
        }

        if( rawRelPath.endsWith( ".jsp" ) )
        {
            return "jsp";
        }

        final int endIndex = rawRelPath.lastIndexOf( '.' );
        final String relPath;
        // users can install tracepoints in files that do not have extensions
        // this just handles it so we have a file/class name for them
        // even though they wont be installed by the agent
        if( endIndex == -1 )
        {
            relPath = rawRelPath;
        }
        else
        {
            relPath = rawRelPath.substring( 0, endIndex );
        }
        if( srcRootArg != null )
        {
            if( relPath.startsWith( srcRootArg ) )
            {
                return Utils.trim( relPath.substring( srcRootArg.length() ), "/" );
            }
        }
        else if( relPath.contains( "/src/main/" ) )
        {
            final String mainDir = relPath.substring( relPath.indexOf( "/src/main/" ) + 11 );
            final int i = mainDir.indexOf( '/' );
            return Utils.trim( mainDir.substring( i ), "/" );
        }
        else if( relPath.contains( "/src/test/" ) )
        {
            final String mainDir = relPath.substring( relPath.indexOf( "/src/test/" ) + 11 );
            final int i = mainDir.indexOf( '/' );
            return Utils.trim( mainDir.substring( i ), "/" );
        }
        final String trim = Utils.trim( relPath, "/" );
        // this is just to ensure the file name is never empty
        // this only happens on non class files such as '.gitignore'
        // rather then return empty name we return the raw path
        if( trim.isEmpty() )
        {
            return Utils.trim( rawRelPath, "/" );
        }
        return trim;
    }
}
