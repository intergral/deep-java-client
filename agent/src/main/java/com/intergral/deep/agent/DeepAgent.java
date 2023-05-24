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

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.plugins.PluginLoader;
import com.intergral.deep.agent.poll.LongPollService;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.resource.ResourceDetector;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.handler.Callback;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepAgent
{
    private final static Logger LOGGER = LoggerFactory.getLogger( DeepAgent.class );
    private final Settings settings;
    private final GrpcService grpcService;
    private final LongPollService pollService;
    private final TracepointConfigService tracepointConfig;
    private final PushService pushService;

    public DeepAgent( final Settings settings, TracepointInstrumentationService tracepointInstrumentationService )
    {
        this.settings = settings;
        this.grpcService = new GrpcService( settings );
        this.pollService = new LongPollService( settings, this.grpcService );
        this.tracepointConfig = new TracepointConfigService( tracepointInstrumentationService );
        this.pushService = new PushService( settings, grpcService );

        Callback.init( settings, tracepointConfig, pushService );
    }

    public void start()
    {
        final Resource resource = ResourceDetector.configureResource( settings, DeepAgent.class.getClassLoader() );
        final PluginLoader.ILoadedPlugins iLoadedPlugins = PluginLoader.loadPlugins();
        this.settings.setPlugins( iLoadedPlugins.plugins() );
        this.settings.setResource( resource.merge( iLoadedPlugins.resource() ) );
        this.grpcService.start();
        this.pollService.start( tracepointConfig );
    }
}
