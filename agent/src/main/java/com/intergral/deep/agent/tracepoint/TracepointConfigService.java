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

package com.intergral.deep.agent.tracepoint;

import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracepointConfigService implements ITracepointConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(TracepointConfigService.class);
  private final TracepointInstrumentationService tracepointInstrumentationService;
  private String currentHash = null;
  private final Collection<TracePointConfig> customTracepoints = new ArrayList<>();
  private Collection<TracePointConfig> installedTracepoints = new ArrayList<>();
  @SuppressWarnings("unused")
  private long lastUpdate;

  public TracepointConfigService(
      final TracepointInstrumentationService tracepointInstrumentationService) {
    this.tracepointInstrumentationService = tracepointInstrumentationService;
  }

  @Override
  public void noChange(long tsNano) {
    LOGGER.debug("No change to tracepoint config.");
    this.lastUpdate = tsNano;
  }

  @Override
  public void configUpdate(long tsNano, String hash, Collection<TracePointConfig> tracepoints) {
    this.currentHash = hash;
    this.installedTracepoints = tracepoints;
    this.lastUpdate = tsNano;
    processChange();
  }

  private void processChange() {
    final List<TracePointConfig> allTracepoints = Stream.concat(this.installedTracepoints.stream(), this.customTracepoints.stream())
        .collect(Collectors.toList());
    this.tracepointInstrumentationService.processBreakpoints(allTracepoints);
  }

  @Override
  public String currentHash() {
    return this.currentHash;
  }

  @Override
  public Collection<TracePointConfig> loadTracepointConfigs(final Collection<String> tracepointId) {
    return Stream.concat(installedTracepoints.stream(), customTracepoints.stream())
        .filter(tracePointConfig -> tracepointId.contains(tracePointConfig.getId()))
        .collect(Collectors.toList());
  }

  public TracePointConfig addCustom(final String path, final int line, final Map<String, String> args,
      final Collection<String> watches) {
    final TracePointConfig tracePointConfig = new TracePointConfig(UUID.randomUUID().toString(), path, line, args, watches);
    this.customTracepoints.add(tracePointConfig);
    this.processChange();
    return tracePointConfig;
  }

  public void removeCustom(final TracePointConfig tracePointConfig) {
    this.customTracepoints.removeIf(current -> current.getId().equals(tracePointConfig.getId()));
  }
}
