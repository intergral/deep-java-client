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

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.api.IDeepLoader;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class Deep
{
    private static Deep DEEP_INSTANCE = null;
    private Object deepService = null;
    private Object reflection = null;


    public static void start()
    {
        Deep.config().start();
    }

    public static Deep getInstance()
    {
        if( DEEP_INSTANCE != null )
        {
            return DEEP_INSTANCE;
        }
        else
        {
            DEEP_INSTANCE = new Deep();
        }
        return DEEP_INSTANCE;
    }

    public static DeepConfigBuilder config()
    {
        return new DeepConfigBuilder();
    }

    public void startWithConfig( final String config )
    {
        getInstance().startDeep( config );
    }

    private void startDeep( final String config )
    {
        try
        {
            loadAgent( config );

            loadAPI();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    private void loadAgent( final String config ) throws Throwable
    {
        final IDeepLoader loader = getLoader();
        final String pid = getPid();

        loader.load( pid, config );
    }


    /**
     * Get the process id for the current process
     *
     * @return the process id
     */
    String getPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        return nameOfRunningVM.substring( 0, nameOfRunningVM.indexOf( '@' ) );
    }


    /**
     * Load the loader for NerdVision
     *
     * @return the loader to use
     * @throws Throwable if we cannot load the loader
     */
    IDeepLoader getLoader() throws Throwable
    {
        final String property = System.getProperty( "deep.loader", DeepLoader.class.getName() );
        final String env = System.getenv( "DEEP_LOADER" );
        final String loader;
        if( env != null )
        {
            loader = env;
        }
        else
        {
            loader = property;
        }

        final Class<?> aClass = Class.forName( loader );
        final Constructor<?> constructor = aClass.getConstructor();
        final Object newInstance = constructor.newInstance();
        return (IDeepLoader) newInstance;
    }

    private void loadAPI()
    {
        if( this.deepService != null )
        {
            // api already loaded
            return;
        }

        try
        {
            final Class<?> aClass = Class.forName( "com.intergral.deep.agent.AgentImpl" );
            final Method registerBreakpointService = aClass.getDeclaredMethod( "loadDeepAPI" );
            final Object invoke = registerBreakpointService.invoke( null );
            if( invoke != null )
            {
                setProxyService( invoke );
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
//            error( "Unable to link nerd.vision proxy service.", e );
        }
    }

    /**
     * Get an instance of the API to allow calling NerdVision directly
     *
     * @param <T> this should be {@link IDeep}
     * @return the new instance or {@link IDeep}
     * @throws IllegalStateException if NerdVision has not been started yet.
     */
    public <T> T api()
    {
        loadAPI();
        if( this.deepService == null )
        {
            throw new IllegalStateException( "Must start Deep first!" );
        }
        return (T) deepService;
    }


    private void setProxyService( Object service )
    {
        final IDeepHook hook = (IDeepHook) service;
        this.deepService = hook.deepService();
        this.reflection = hook.reflectionService();
    }

    public <T> T reflection()
    {
        loadAPI();
        if( this.reflection == null )
        {
            throw new IllegalStateException( "Must start Deep first!" );
        }
        return (T) reflection;
    }
}
