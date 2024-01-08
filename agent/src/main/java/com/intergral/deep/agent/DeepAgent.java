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
import com.intergral.deep.agent.api.IRegistration;
import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.api.tracepoint.ITracepoint;
import com.intergral.deep.agent.api.tracepoint.ITracepoint.ITracepointRegistration;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.plugins.PluginSpiLoader;
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

/**
 * This is the agent that is provided via the API, and is what holds all deep together.
 */
public class DeepAgent implements IDeep {

  private final Settings settings;
  private final GrpcService grpcService;
  private final LongPollService pollService;
  private final TracepointConfigService tracepointConfig;

  /**
   * Create a new deep agent.
   *
   * @param settings                         the settings
   * @param tracepointInstrumentationService the tracepoint instrumentation service
   */
  public DeepAgent(final Settings settings,
      TracepointInstrumentationService tracepointInstrumentationService) {
    this.settings = settings;
    this.grpcService = new GrpcService(settings);
    this.pollService = new LongPollService(settings, this.grpcService);
    this.tracepointConfig = new TracepointConfigService(tracepointInstrumentationService);
    final PushService pushService = new PushService(grpcService);

    Callback.init(settings, tracepointConfig, pushService);
  }

  /**
   * Start deep.
   */
  public void start() {
    final List<IDeepPlugin> iLoadedPlugins = PluginSpiLoader
        .loadPlugins(settings, Reflection.getInstance(), DeepAgent.class.getClassLoader());
    final Resource resource = ResourceDetector.configureResource(this.settings, iLoadedPlugins);
    this.settings.setPlugins(iLoadedPlugins);
    this.settings.setResource(Resource.DEFAULT.merge(resource));
    this.grpcService.start();
    this.pollService.start(tracepointConfig);
  }

  @Override
  public String getVersion() {
    return DeepVersion.VERSION;
  }

  @Override
  public IRegistration<IDeepPlugin> registerPlugin(final IDeepPlugin plugin) {
    plugin.configure(this.settings, Reflection.getInstance());
    return this.settings.addPlugin(plugin);
  }

  @Override
  public ITracepointRegistration registerTracepoint(final String path, final int line) {
    return registerTracepoint(path, line, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public ITracepointRegistration registerTracepoint(final String path, final int line, final Map<String, String> args,
      final Collection<String> watches, final Collection<MetricDefinition> metricDefinitions) {
    final TracePointConfig tracePointConfig = this.tracepointConfig.addCustom(path, line, args, watches, metricDefinitions);
    return new ITracepointRegistration() {
      @Override
      public void unregister() {
        tracepointConfig.removeCustom(tracePointConfig);
      }

      @Override
      public ITracepoint get() {
        return new ITracepoint() {
          @Override
          public String path() {
            return path;
          }

          @Override
          public int line() {
            return line;
          }

          @Override
          public Map<String, String> args() {
            return args;
          }

          @Override
          public Collection<String> watches() {
            return watches;
          }

          @Override
          public String id() {
            return tracePointConfig.getId();
          }
        };
      }
    };
  }

  @Override
  public boolean isEnabled() {
    return this.settings.isActive();
  }

  @Override
  public synchronized void setEnabled(final boolean enabled) {
    // we are already the desired state - so do nothing
    if (isEnabled() == enabled) {
      return;
    }

    // update config to new state
    this.settings.setActive(enabled);

    // if we are disabling then we need to clear configs
    if (!enabled) {
      this.tracepointConfig.configUpdate(0, null, Collections.emptyList());
    }
  }

  public void shutdown() {
    this.grpcService.shutdown();
  }
}
