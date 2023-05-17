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

package com.intergral.deep.agent.settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Settings
{

    private final Properties properties;

    private Settings( Properties properties )
    {
        this.properties = properties;
    }

    public static Settings build( final Map<String, String> agentArgs )
    {
        final InputStream resourceAsStream = Settings.class.getResourceAsStream( "/deep_settings.properties" );
        return build( agentArgs, resourceAsStream );
    }

    static Settings build( final Map<String, String> agentArgs, final InputStream stream )
    {
        final Properties properties = new Properties();
        try( final InputStream resourceAsStream = stream; )
        {
            properties.load( resourceAsStream );
        }
        catch( IOException e )
        {
            // logging is not initialized until after the settings class
            e.printStackTrace();
        }

        for( final Map.Entry<Object, Object> propEntry : properties.entrySet() )
        {
            final String key = String.valueOf( propEntry.getKey() );
            final String systemProp = readSystemProperty( key );
            final String envProp = readEnvProperty( key );
            final String agentKey = agentArgs.get( key );

            if( agentKey != null )
            {
                properties.put( key, agentKey );
            }
            else if( envProp != null )
            {
                properties.put( key, envProp );
            }
            else if( systemProp != null )
            {
                properties.put( key, systemProp );
            }
        }


        for( Map.Entry<String, String> agentArg : agentArgs.entrySet() )
        {
            properties.put( agentArg.getKey(), agentArg.getValue() );
        }
        return new Settings( properties );
    }

    private static String readEnvProperty( final String key )
    {
        return System.getenv( "DEEP_" + key.toUpperCase().replaceAll( "\\.", "_" ) );
    }

    private static String readSystemProperty( final String key )
    {
        return System.getProperty( "deep." + key );
    }

    @SuppressWarnings("unchecked")
    public static <T> T coerc( final String str, final Class<T> type )
    {
        if( str == null )
        {
            return null;
        }

        if( type == Boolean.class || type == boolean.class )
        {
            return (T) Boolean.valueOf( str );
        }
        else if( type == Integer.class || type == int.class )
        {
            return (T) Integer.valueOf( str );
        }
        else if( type == Long.class || type == long.class )
        {
            return (T) Long.valueOf( str );
        }
        else if( type == String.class )
        {
            return (T) str;
        }
        else if( type == Double.class || type == double.class )
        {
            return (T) Double.valueOf( str );
        }
        else if( type == Float.class || type == float.class )
        {
            return (T) Float.valueOf( str );
        }
        else if( type == List.class )
        {
            // Java doesnt allow us to know what type of List, so they can only be strings
            return (T) makeList( str );
        }
        else if( type == Map.class )
        {
            final List<String> strs = makeList( str );
            final Map<String, String> map = new HashMap<>();

            for( final String s : strs )
            {
                final String[] split = s.split( "=" );
                if( split.length == 2 )
                {
                    map.put( split[0], split[1] );
                }
            }

            return (T) map;
        }
        else if( type == Level.class )
        {
            if( str.equalsIgnoreCase( "debug" ) )
            {
                return (T) Level.FINEST;
            }
            return (T) Level.parse( str );
        }
        else if( type == Pattern.class )
        {
            return (T) Pattern.compile( str );
        }
        else if( type == URL.class )
        {
            try
            {
                return (T) new URL( str );
            }
            catch( MalformedURLException mue )
            {
                throw new RuntimeException( str + " is not a valid URL" );
            }
        }

        throw new IllegalArgumentException( "Cannot coerc " + str + " to " + type );
    }

    private static List<String> makeList( final String str )
    {
        final String trimmed = str.trim();
        if( trimmed.isEmpty() )
        {
            return Collections.emptyList();
        }

        String[] split = trimmed.split( "," );
        // Either 1 key only or using different format
        if( split.length == 1 )
        {
            split = trimmed.split( ";" );
        }

        return Arrays.asList( split );
    }

    public <T> T getSettingAs( String key, Class<T> clazz )
    {
        final String property = this.properties.getProperty( key );

        return coerc( property, clazz );
    }
}
