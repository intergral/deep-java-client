package com.intergral.deep.agent;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.agent.api.reflection.IReflection;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.jar.JarFile;

public class AgentImpl
{

    public static void startup( final JarFile jarFile, final Instrumentation inst, final Map<String, String> args )
    {
        System.out.println( "Starting Deep Agent" );
    }

    public static Object loadDeepAPI()
    {
        return new IDeepHook()
        {

            @Override
            public IDeep deepService()
            {
                return new IDeep()
                {
                    @Override
                    public String getVersion()
                    {
                        return "0.0.1";
                    }
                };
            }

            @Override
            public IReflection reflectionService()
            {
                return Utils.getReflection();
            }
        };
    }
}
