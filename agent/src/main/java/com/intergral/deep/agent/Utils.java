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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Utils
{
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


    /**
     * Get the curren time in nanoseconds from epoch
     * <p>
     * Long will wrap when the date exceeds Saturday, 12 April 2262 00:47:16.854 GMT+01:00 DST
     *
     * @return the epoch time in nanoseconds
     */
    public static long currentTimeNanos()
    {
        final Instant now = Instant.now();
        final String format = String.format( "%d%d", now.getEpochSecond(), now.getNano() );
        return Long.parseLong( format );
    }

    public static <T> Map<String, T> newMap( final Map<String, T> map )
    {
        if( map == null )
        {
            return Collections.emptyMap();
        }
        return new HashMap<>( map );
    }


    /**
     * FROM: https://stackoverflow.com/a/38947571
     *
     * @param str    the string to search
     * @param suffix the value to serch for
     * @return true if {@code str} ends with {@code suffix}, disregarding case sensitivity
     */
    public static boolean endsWithIgnoreCase( String str, String suffix )
    {
        int suffixLength = suffix.length();
        return str.regionMatches( true, str.length() - suffixLength, suffix, 0, suffixLength );
    }

    public static String valueOf( final Object obj )
    {
        if( obj == null )
        {
            return "null";
        }

        String hash;
        try
        {
            final String tmp = String.valueOf( obj );
            // FR-5298 - Protected again NullPointerException when stepping in
            //Stringbuilder.<init>
            tmp.length();
            return tmp;
        }
        catch( final Throwable e1 )
        {
            // From Object.toString();
            hash = String.valueOf( System.identityHashCode( obj ) );
            return obj.getClass().getName() + "@" + hash + " toString() failed";
        }
    }


    public static String trim( String str, final String trimStr )
    {
        while( str.startsWith( trimStr ) )
        {
            str = str.substring( 1 );
        }
        return str;
    }


    public static ITrimResult trim( final String str, final int maxLength )
    {
        if( str.length() > maxLength )
        {
            return new ITrimResult()
            {
                @Override
                public String value()
                {
                    return str.substring( 0, maxLength );
                }


                @Override
                public boolean truncated()
                {
                    return true;
                }
            };
        }
        return new ITrimResult()
        {
            @Override
            public String value()
            {
                return str;
            }


            @Override
            public boolean truncated()
            {
                return false;
            }
        };
    }


    /**
     * The result of a trim operation
     */
    public interface ITrimResult
    {
        /**
         * The value to use, might be truncated
         *
         * @return the value
         */
        String value();


        /**
         * Has the value been truncated
         *
         * @return {@code true} if the value was truncated
         */
        boolean truncated();
    }
}
