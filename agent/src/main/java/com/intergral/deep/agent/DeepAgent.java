/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent;

import com.intergral.deep.agent.api.DeepVersion;
import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.IPlugin.IPluginRegistration;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.tracepoint.ITracepoint.ITracepointRegistration;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.plugins.PluginLoader;
import com.intergral.deep.agent.poll.LongPollService;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.resource.ResourceDetector;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.handler.Callback;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DeepAgent implements IDeep {

  private final Settings settings;
  private final GrpcService grpcService;
  private final LongPollService pollService;
  private final TracepointConfigService tracepointConfig;
  private final PushService pushService;

  public DeepAgent(final Settings settings,
      TracepointInstrumentationService tracepointInstrumentationService) {
    this.settings = settings;
    this.grpcService = new GrpcService(settings);
    this.pollService = new LongPollService(settings, this.grpcService);
    this.tracepointConfig = new TracepointConfigService(tracepointInstrumentationService);
    this.pushService = new PushService(settings, grpcService);

    Callback.init(settings, tracepointConfig, pushService);
  }

  public void start() {
    final Resource resource = ResourceDetector.configureResource(settings,
        DeepAgent.class.getClassLoader());
    final List<IPlugin> iLoadedPlugins = PluginLoader.loadPlugins(settings);
    this.settings.setPlugins(iLoadedPlugins);
    this.settings.setResource(Resource.DEFAULT.merge(resource));
    this.grpcService.start();
    this.pollService.start(tracepointConfig);
  }

  @Override
  public String getVersion() {
    return DeepVersion.VERSION;
  }

  public IPluginRegistration registerPlugin(final IPlugin plugin) {
    this.settings.addPlugin(plugin);
    return () -> DeepAgent.this.settings.removePlugin(plugin);
  }

  @Override
  public ITracepointRegistration registerTracepoint(final String path, final int line) {
    return registerTracepoint(path, line, Collections.emptyMap(), Collections.emptyList());
  }

  @Override
  public ITracepointRegistration registerTracepoint(final String path, final int line, final Map<String, String> args,
      final Collection<String> watches) {
    final TracePointConfig tracePointConfig = this.tracepointConfig.addCustom(path, line, args, watches);
    return () -> DeepAgent.this.tracepointConfig.removeCustom(tracePointConfig);
  }
}
