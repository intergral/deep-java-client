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

package com.intergral.deep.agent;

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.reflect.ReflectionImpl;

public class Utils
{

    private final static IReflection reflection;

    static
    {
        if( getVersion() >= 9 )
        {
            reflection = new com.intergral.deep.reflect.Java9ReflectionImpl();
        }
        else
        {
            reflection = new ReflectionImpl();
        }
    }

    public static int getVersion()
    {
        String version = System.getProperty( "java.version" );
        return extractVersion( version );
    }

    static int extractVersion( String version )
    {
        if( version.startsWith( "1." ) )
        {
            version = version.substring( 2, 3 );
        }
        else
        {
            int dot = version.indexOf( "." );
            if( dot != -1 )
            {
                version = version.substring( 0, dot );
            }
        }
        return Integer.parseInt( version );
    }

    public static IReflection getReflection()
    {
        return reflection;
    }
}
