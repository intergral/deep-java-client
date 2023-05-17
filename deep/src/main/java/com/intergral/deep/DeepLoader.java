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

package com.intergral.deep;

import com.intergral.deep.api.IDeepLoader;
import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DeepLoader implements IDeepLoader
{
    @Override
    public void load( final String pid, final Map<String, Object> config ) throws Throwable
    {
        final File agentJar = getAgentJar();
        final File tools = getToolsJar();
        if( agentJar == null )
        {
            throw new RuntimeException( "Cannot find jar." );
        }
        ByteBuddyAgent.attach( agentJar, pid, configAsArgs( config ), new ShippedToolsJarProvider( tools ) );
    }


    /**
     * Convert the config to a string
     *
     * @param config the config to parse
     * @return the config as a string
     */
    String configAsArgs( final Map<String, Object> config )
    {
        final StringBuilder stringBuilder = new StringBuilder();
        for( Map.Entry<String, Object> entry : config.entrySet() )
        {
            if( entry.getValue() == null )
            {
                continue;
            }
            if( stringBuilder.length() != 0 )
            {
                stringBuilder.append( ',' );
            }
            if( entry.getValue() instanceof Map )
            {
                //noinspection unchecked
                stringBuilder.append( entry.getKey() )
                        .append( '=' )
                        .append( configAsArgs( (Map<String, Object>) entry.getValue() ) );
            }
            else
            {
                stringBuilder.append( entry.getKey() ).append( '=' ).append( entry.getValue() );
            }
        }
        return stringBuilder.toString();
    }


    /**
     * Load the tools jar as a file object. We use the tools jar to dynamically attach to the vm. A version of tools is shipped if
     * we are running a JRE.
     *
     * @return the {@link File} for the tools jar or {@code null}
     */
    private File getToolsJar()
    {
        final InputStream resourceAsStream = Deep.class.getResourceAsStream( "/tools.jar" );
        final String pathname = extractLibrary( resourceAsStream );
        if( pathname != null )
        {
            return new File( pathname );
        }
        return null;
    }


    /**
     * Load the agent jar to a file
     *
     * @return the {@link File} object for the agent
     */
    private File getAgentJar()
    {
        final InputStream resourceAsStream = getAgentJarStream();
        final String pathname = extractLibrary( resourceAsStream );
        if( pathname != null )
        {
            return new File( pathname );
        }
        return null;
    }


    /**
     * Log the agent file as a stream
     *
     * @return the stream to use, or {@code null}
     */
    private InputStream getAgentJarStream()
    {
        // this is pretty much just for testing, see Example
        final String property = System.getProperty( "nv.jar.path" );
        if( property != null )
        {
            try
            {
                return new FileInputStream( property );
            }
            catch( FileNotFoundException e )
            {
                System.err.println( "Unable to load NerdVision jar from path: " + property );
                e.printStackTrace( System.err );
                return null;
            }
        }
        return Deep.class.getResourceAsStream( "/nerdvision-agent.jar" );
    }


    /**
     * Extract a stream to a temp file and return the absolute file path
     *
     * @param inputStream the stream to extract
     * @return the absolute file path to the extracted library
     */
    private String extractLibrary( final InputStream inputStream )
    {
        if( inputStream == null )
        {
            return null;
        }

        FileOutputStream fileOutputStream = null;
        try
        {
            final File tempFile = File.createTempFile( "nerdvision", "agent" );

            fileOutputStream = new FileOutputStream( tempFile );

            byte[] buf = new byte[1024];

            int len;
            while( (len = inputStream.read( buf )) > 0 )
            {
                fileOutputStream.write( buf, 0, len );
            }

            fileOutputStream.close();
            inputStream.close();
            tempFile.setExecutable( true, true );

            return tempFile.getAbsolutePath();
        }
        catch( IOException e )
        {
            return null;
        }
        finally
        {
            if( fileOutputStream != null )
            {
                try
                {
                    fileOutputStream.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }
    }

}
