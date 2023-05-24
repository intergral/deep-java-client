/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.asm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;

public class TransformerUtils
{
    private static final List<String> EXCLUDE_PACKAGES = Collections.emptyList();
    private static final List<String> EXCLUDE_CONTAINS = Collections.emptyList();


    public static boolean storeUnsafe( String path, byte[] original, byte[] transformed, String className )
    {
        try
        {
            if( path == null || path.isEmpty() )
            {
                return false;
            }
            store( path, original, transformed, className );
            return true;
        }
        catch( IOException ex )
        {
            return false;
        }
    }


    public static void store( String path, byte[] originalBytes, byte[] transformedBytes, String className )
            throws FileNotFoundException, IOException
    {
        if( path == null )
        {
            return;
        }
        File dir = new File( path );
        dir.mkdirs();
        dir.setReadable( true, false );
        dir.setExecutable( true, false );
        dir.setWritable( true, false );

        String filename = className.replace( "/", "_" );

        {
            File org = new File( dir, filename + ".class" );
            if( org.exists() )
            {
                org.delete();
            }
            FileOutputStream out = new FileOutputStream( org, false );
            out.write( originalBytes );
            out.close();

            org.setReadable( true, false );
            org.setWritable( true, false );
            org.setExecutable( true, false );
        }

        {
            File transformed = new File( dir, filename + "_trnfm.class" );
            if( transformed.exists() )
            {
                transformed.delete();
            }
            FileOutputStream out = new FileOutputStream( transformed, false );
            out.write( transformedBytes );
            out.close();

            transformed.setReadable( true, false );
            transformed.setWritable( true, false );
            transformed.setExecutable( true, false );
        }
    }


    public static boolean isExcludedClass( final Class<?> loadedClass )
    {
        return isExcludedClass( loadedClass.getName() );
    }


    public static boolean isExcludedClass( final String classname )
    {
        for( final String pkg : EXCLUDE_PACKAGES )
        {
            if( classname.startsWith( pkg ) )
            {
                return true;
            }
        }

        for( final String contain : EXCLUDE_CONTAINS )
        {
            if( classname.contains( contain ) )
            {
                return true;
            }
        }

        return false;
    }


    public static boolean isAbstract( final int access )
    {
        return (access & ACC_ABSTRACT) == ACC_ABSTRACT;
    }
}
