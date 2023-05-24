/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.objectweb.asm.Opcodes.ASM7;

/**
 * Use getResource and getResourceAsStream from class loaders to get Debug Source of loaded classes.
 * 
 * @author nwightma
 * @since 1.0.2
 */
public class SmapUtils
{

    private SmapUtils()
    {
    }


    public static String lookUp( final Class c )
    {
        final String clazzFile = c.getName().replace( '.', '/' ) + ".class";

        ClassLoader loader = c.getClassLoader();
        while( loader != null )
        {
            final URL resource = loader.getResource( clazzFile );
            if( resource != null )
            {
                try
                {
                    final InputStream in = resource.openStream();
                    return parseStream( in );
                }
                catch( Throwable t )
                {
                    t.printStackTrace();
                }
            }
            else
            {
                try
                {
                    final InputStream in = loader.getResourceAsStream( clazzFile );
                    if( in != null )
                    {
                        return parseStream( in );
                    }
                }
                catch( Throwable t )
                {
                    t.printStackTrace();
                }
            }

            // move up
            loader = loader.getParent();
        }

        final URL resource = ClassLoader.getSystemResource( clazzFile );
        if( resource != null )
        {
            try
            {
                final InputStream in = resource.openStream();
                return parseStream( in );
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
        }

        return null;
    }


    public static String parseBytes( final byte[] bytes ) throws IOException
    {
        final ClassReader reader = new ClassReader( bytes );
        return scan( reader );
    }


    public static String parseStream( final InputStream in ) throws IOException
    {
        final ClassReader reader = new ClassReader( in );
        return scan( reader );
    }


    protected static String scan( ClassReader reader )
    {
        final Visitor v = new Visitor();
        reader.accept( v, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES );
        return v.getDebug();
    }


    public static String scanSource( ClassReader reader )
    {
        final Visitor v = new Visitor();
        reader.accept( v, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES );
        return v.getSource();
    }

    public static class Visitor extends ClassVisitor
    {
        private String source;
        private String debug;


        public Visitor()
        {
            super( ASM7 );
        }


        @Override
        public void visitSource( String source, String debug )
        {
            super.visitSource( source, debug );

            this.source = source;
            this.debug = debug;
        }


        public String getSource()
        {
            return source;
        }


        public String getDebug()
        {
            return debug;
        }

    }
}