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

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is an implementation of the {@link net.bytebuddy.agent.ByteBuddyAgent.AttachmentProvider} that uses the tools jar that we
 * ship with the agent.
 */
public class ShippedToolsJarProvider implements ByteBuddyAgent.AttachmentProvider
{
    private final File tools;


    /**
     * Create a new provider
     *
     * @param tools the file object for the tools jar to use
     */
    public ShippedToolsJarProvider( final File tools )
    {
        this.tools = tools;
    }


    @Override
    public Accessor attempt()
    {
        // first just try the normal default loaders
        final Accessor attempt = DEFAULT.attempt();
        if( attempt.isAvailable() )
        {
            return attempt;
        }

        // if the tools jar is not available then return a failure.
        if( tools == null )
        {
            return Accessor.Unavailable.INSTANCE;
        }
        else
        {
            // else load the tools jar and return an accessor to use it
            try
            {
                return Accessor.Simple.of( new URLClassLoader( new URL[] { tools.toURI().toURL() }, null ), tools );
            }
            catch( MalformedURLException exception )
            {
                throw new IllegalStateException( "Could not represent " + tools + " as URL" );
            }
        }
    }
}
