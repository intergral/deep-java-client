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
