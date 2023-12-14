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

package com.intergral.deep.agent.poll;

import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.ITracepointConfig;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.proto.common.v1.AnyValue;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.poll.v1.PollConfigGrpc;
import com.intergral.deep.proto.poll.v1.PollRequest;
import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.poll.v1.ResponseType;
import com.intergral.deep.proto.tracepoint.v1.Metric;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This service deals with polling the remote service for tracepoint configs.
 */
public class LongPollService implements ITimerTask {

  private final Settings settings;
  private final GrpcService grpcService;
  private final DriftAwareThread thread;
  private ITracepointConfig tracepointConfig;

  /**
   * Create a new service.
   *
   * @param settings    the deep settings
   * @param grpcService the deep grpc service
   */
  public LongPollService(final Settings settings, final GrpcService grpcService) {
    this.settings = settings;
    this.grpcService = grpcService;
    this.thread = new DriftAwareThread(LongPollService.class.getSimpleName(),
        this,
        settings.getSettingAs("poll.timer", Integer.class));
  }

  void setTracepointConfig(final ITracepointConfig tracepointConfig) {
    this.tracepointConfig = tracepointConfig;
  }

  public void start(final ITracepointConfig tracepointConfig) {
    this.tracepointConfig = tracepointConfig;
    thread.start(0);
  }

  @Override
  public void run(long now) {
    if (!this.settings.isActive()) {
      // we have been disabled so skip this poll
      // we will pause like normal and try again later
      return;
    }
    final PollConfigGrpc.PollConfigBlockingStub blockingStub = this.grpcService.pollService();

    final PollRequest.Builder builder = PollRequest.newBuilder();
    if (this.tracepointConfig.currentHash() != null) {
      builder.setCurrentHash(this.tracepointConfig.currentHash());
    }

    final PollRequest pollRequest = builder
        .setTsNanos(now)
        .setResource(buildResource())
        .build();

    final PollResponse response = blockingStub.poll(pollRequest);
    // check we are still active
    if (!this.settings.isActive()) {
      return;
    }
    if (response.getResponseType() == ResponseType.NO_CHANGE) {
      this.tracepointConfig.noChange(response.getTsNanos());
    } else {
      this.tracepointConfig.configUpdate(response.getTsNanos(),
          response.getCurrentHash(),
          convertResponse(response.getResponseList()));
    }
  }

  private Collection<TracePointConfig> convertResponse(
      List<com.intergral.deep.proto.tracepoint.v1.TracePointConfig> responseList) {
    return responseList.stream()
        .map(tracePointConfig -> new TracePointConfig(tracePointConfig.getID(),
            tracePointConfig.getPath(),
            tracePointConfig.getLineNumber(),
            Collections.unmodifiableMap(new HashMap<>(tracePointConfig.getArgsMap())),
            Collections.unmodifiableCollection(tracePointConfig.getWatchesList()),
            Collections.unmodifiableList(covertMetrics(tracePointConfig.getMetricsList()))))
        .collect(Collectors.toList());
  }

  List<MetricDefinition> covertMetrics(final List<Metric> metricsList) {
    if (metricsList == null || metricsList.isEmpty()) {
      return Collections.emptyList();
    }
    return metricsList.stream().map(
            metric -> new MetricDefinition(metric.getName(), metric.getTagsMap(), metric.getType().toString(), metric.getExpression(),
                namespaceOrDefault(metric.getNamespace()), helpOrDefault(metric.getHelp(), metric.getExpression()), metric.getUnit()))
        .collect(Collectors.toList());
  }

  private String namespaceOrDefault(final String namespace) {
    if (namespace == null || namespace.trim().isEmpty()) {
      return "deep_agent";
    }
    return namespace;
  }

  private String helpOrDefault(final String help, final String expression) {
    if (help == null || help.trim().isEmpty()) {
      return "Metric generated from expression: " + expression;
    }
    return help;
  }

  private com.intergral.deep.proto.resource.v1.Resource buildResource() {
    final Resource resource = this.settings.getResource();
    return com.intergral.deep.proto.resource.v1.Resource.newBuilder()
        .addAllAttributes(
            resource.getAttributes().entrySet().stream().map(entry -> KeyValue.newBuilder()
                .setKey(entry.getKey())
                .setValue(AnyValue.newBuilder().setStringValue(
                    String.valueOf(entry.getValue())).build())
                .build()).collect(Collectors.toList()))
        .build();
  }

  @Override
  public long callback(long duration, long nextExecutionTime) {
    return nextExecutionTime;
  }
}
