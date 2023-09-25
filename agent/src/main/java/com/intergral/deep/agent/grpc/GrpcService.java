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

package com.intergral.deep.agent.grpc;

import com.intergral.deep.agent.Reflection;
import com.intergral.deep.agent.api.auth.AuthProvider;
import com.intergral.deep.agent.api.auth.IAuthProvider;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.proto.poll.v1.PollConfigGrpc;
import com.intergral.deep.proto.tracepoint.v1.SnapshotServiceGrpc;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.NameResolverRegistry;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.PreferHeapByteBufAllocator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service handles the grpc channel and attaching the metadata to the outbound services.
 */
public class GrpcService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcService.class);

  private final Settings settings;
  private ManagedChannel channel;

  public GrpcService(final Settings settings) {
    this.settings = settings;
  }

  /**
   * Start the grpc service and connect the channel.
   */
  public void start() {
    try {
      setupChannel();
    } catch (Exception e) {
      LOGGER.error("Error setting up GRPC channel", e);
    }
  }

  /**
   * Shutdown the grpc channel.
   */
  public void shutdown() {
    if (this.channel == null) {
      return;
    }
    try {
      this.channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Could not shutdown cleanly.", e);
    }
  }

  private void setupChannel() {
    final String serviceHost = this.settings.getServiceHost();
    final int servicePort = this.settings.getServicePort();
    LOGGER.debug("Connecting to server {}:{}", serviceHost, servicePort);

    // we have to set these as the service loaders are not available
    NameResolverRegistry.getDefaultRegistry().register(new DnsNameResolverProvider());
    LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());

    // Create the channel pointing to the server
    NettyChannelBuilder ncBuilder = NettyChannelBuilder.forAddress(serviceHost, servicePort)
        .keepAliveTimeout(60, TimeUnit.SECONDS)
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(true)
        .enableRetry()
        .executor(Executors.newFixedThreadPool(1))
        .maxRetryAttempts(Integer.MAX_VALUE);

    final ByteBufAllocator allocator;
    if (this.settings.getSettingAs("grpc.allocator", String.class).equals("default")) {
      allocator = ByteBufAllocator.DEFAULT;
    } else if (this.settings.getSettingAs("grpc.allocator", String.class).equals("pooled")) {
      allocator = PooledByteBufAllocator.DEFAULT;
    } else {
      allocator = UnpooledByteBufAllocator.DEFAULT;
    }

    if (this.settings.getSettingAs("grpc.heap.allocator", Boolean.class)) {
      ncBuilder.withOption(ChannelOption.ALLOCATOR, new PreferHeapByteBufAllocator(allocator));
    } else {
      ncBuilder.withOption(ChannelOption.ALLOCATOR, allocator);
    }

    // Select secure or not
    if (this.settings.getSettingAs(ISettings.KEY_SERVICE_SECURE, Boolean.class)) {
      ncBuilder.useTransportSecurity();
    } else {
      ncBuilder.usePlaintext();
    }

    channel = ncBuilder.build();
  }

  private ManagedChannel getChannel() {
    if (this.channel == null) {
      try {
        setupChannel();
      } catch (Exception e) {
        LOGGER.debug("Error setting up GRPC channel", e);
      }
    }
    return channel;
  }

  /**
   * Get the grpc service for polling configs.
   *
   * @return the service to use
   */
  public PollConfigGrpc.PollConfigBlockingStub pollService() {
    final PollConfigGrpc.PollConfigBlockingStub blockingStub = PollConfigGrpc.newBlockingStub(
        getChannel());

    final Metadata metadata = buildMetaData();

    return blockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(
        metadata));
  }

  /**
   * Get the grpc service for sending snapshots.
   *
   * @return the service to use
   */
  public SnapshotServiceGrpc.SnapshotServiceStub snapshotService() {
    final SnapshotServiceGrpc.SnapshotServiceStub snapshotServiceStub = SnapshotServiceGrpc.newStub(
        getChannel());
    final Metadata metadata = buildMetaData();

    return snapshotServiceStub.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
  }

  private Metadata buildMetaData() {
    final IAuthProvider provider = AuthProvider.provider(this.settings, Reflection.getInstance());
    final Map<String, String> headers = provider.provide();

    final Metadata metadata = new Metadata();
    for (Map.Entry<String, String> header : headers.entrySet()) {
      metadata.put(Metadata.Key.of(header.getKey(), Metadata.ASCII_STRING_MARSHALLER),
          header.getValue());
    }
    return metadata;
  }
}
