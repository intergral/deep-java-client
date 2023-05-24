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

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.tracepoint.inst.InstrumentationService;
import com.intergral.deep.agent.logging.Logger;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;

import java.lang.instrument.Instrumentation;
import java.util.Map;

public class AgentImpl
{

    public static void startup( final Instrumentation inst, final Map<String, String> args )
    {
        final Settings settings = Settings.build( args );
        final org.slf4j.Logger logger = Logger.configureLogging( settings );
        final TracepointInstrumentationService tracepointInstrumentationService = InstrumentationService.init( inst, settings );
        final DeepAgent deepAgent = new DeepAgent( settings, tracepointInstrumentationService );

        deepAgent.start();
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
                return ReflectionUtils.getReflection();
            }
        };
    }
}
