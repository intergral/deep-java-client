package com.intergral.deep.agent.tracepoint.inst.jsp;

import java.io.IOException;
import java.util.List;

public class JSPUtils
{
    private JSPUtils()
    {
    }


    public static boolean isJspClass( final String jspSuffix,
                                      final List<String> jspPackages,
                                      final String loadedClassName )
    {
        return getJspClassname( jspSuffix, jspPackages, loadedClassName ) != null;
    }


    private static String getJspClassname( final String jspSuffix,
                                           final List<String> jspPackages,
                                           final String className )
    {
        if( jspSuffix.isEmpty() || className.endsWith( jspSuffix ) )
        {
            for( final String jspPackage : jspPackages )
            {
                if( className.startsWith( jspPackage ) )
                {
                    return className.substring( jspPackage.length() + 1 );
                }
            }
            if( className.startsWith( "/" ) )
            {
                return className.substring( 1 );
            }
        }
        return null;
    }



    public static SourceMap getSourceMap( final Class<?> c )
    {
        try
        {
            final String rtn = SmapUtils.lookUp( c );
            final SourceMapParser parser;
            if( rtn != null )
            {
                parser = new SourceMapParser( rtn );
                return parser.parse();
            }
            return null;
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
            return null;
        }
    }


    public static SourceMap getSourceMap( byte[] classfileBuffer )
    {
        try
        {
            final String rtn = SmapUtils.parseBytes( classfileBuffer );
            final SourceMapParser parser = new SourceMapParser( rtn );
            return parser.parse();
        }
        catch( IOException ioe )
        {
            ioe.printStackTrace();
            return null;
        }
    }
}
