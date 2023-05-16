package com.intergral.deep;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.api.IDeepLoader;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;

public class Deep
{
    private static Deep DEEP_INSTANCE = null;
    private Object deepService = null;
    private Object reflection = null;


    public static void start()
    {
        Deep.getInstance().startDeep();
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

    private void startDeep()
    {
        try
        {
            loadAgent();

            loadAPI();
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
    }

    private void loadAgent() throws Throwable
    {
        final IDeepLoader loader = getLoader();
        final String pid = getPid();

        loader.load( pid, new HashMap<>() );
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
        final String property = System.getProperty( "nv.loader", DeepLoader.class.getName() );
        final String env = System.getenv( "NV_LOADER" );
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
