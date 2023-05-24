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

package com.intergral.deep.agent.grpc;

import com.intergral.deep.agent.api.auth.AuthProvider;
import com.intergral.deep.agent.api.auth.IAuthProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GrpcService
{
    private final static Logger LOGGER = LoggerFactory.getLogger( GrpcService.class );

    private final Settings settings;
    private ManagedChannel channel;

    public GrpcService( final Settings settings )
    {
        this.settings = settings;
    }

    public void start()
    {
        try
        {
            setupChannel();
        }
        catch( Exception e )
        {
            LOGGER.debug( "Error setting up GRPC channel", e );
        }
    }

    private void setupChannel()
    {
        final String serviceHost = this.settings.getServiceHost();
        final int servicePort = this.settings.getServicePort();
        LOGGER.debug( "Connecting to server {}:{}", serviceHost, servicePort );

        // we have to set these as the service loaders are not available
        NameResolverRegistry.getDefaultRegistry().register( new DnsNameResolverProvider() );
        LoadBalancerRegistry.getDefaultRegistry().register( new PickFirstLoadBalancerProvider() );

        // Create the channel pointing to the server
        NettyChannelBuilder ncBuilder = NettyChannelBuilder.forAddress( serviceHost, servicePort )
                .keepAliveTimeout( 60, TimeUnit.SECONDS )
                .keepAliveTime( 30, TimeUnit.SECONDS )
                .keepAliveWithoutCalls( true )
                // we have to set these as the service loaders are not available
//                .nameResolverFactory( new DnsNameResolverProvider() )
//                .loadBalancerFactory( new PickFirstLoadBalancerProvider() )
                .enableRetry()
                .executor( Executors.newFixedThreadPool( 1 ) )
                .maxRetryAttempts( Integer.MAX_VALUE );

        final ByteBufAllocator allocator;
        if( this.settings.getSettingAs( "grpc.allocator", String.class ).equals( "default" ) )
        {
            allocator = ByteBufAllocator.DEFAULT;
        }
        else if( this.settings.getSettingAs( "grpc.allocator", String.class ).equals( "pooled" ) )
        {
            allocator = PooledByteBufAllocator.DEFAULT;
        }
        else
        {
            allocator = UnpooledByteBufAllocator.DEFAULT;
        }

        if( this.settings.getSettingAs( "grpc.heap.allocator", Boolean.class ) )
        {
            ncBuilder.withOption( ChannelOption.ALLOCATOR, new PreferHeapByteBufAllocator( allocator ) );
        }
        else
        {
            ncBuilder.withOption( ChannelOption.ALLOCATOR, allocator );
        }

        // Select secure or not
        if( this.settings.getSettingAs( "service.secure", Boolean.class ) )
        {
            ncBuilder.useTransportSecurity();
        }
        else
        {
            ncBuilder.usePlaintext();
        }

        channel = ncBuilder.build();
    }

    private ManagedChannel getChannel()
    {
        if( this.channel == null )
        {
            try
            {
                setupChannel();
            }
            catch( Exception e )
            {
                LOGGER.debug( "Error setting up GRPC channel", e );
            }
        }
        return channel;
    }

    public PollConfigGrpc.PollConfigBlockingStub pollService()
    {
        final PollConfigGrpc.PollConfigBlockingStub blockingStub = PollConfigGrpc.newBlockingStub( getChannel() );

        final Metadata metadata = buildMetaData();

        return blockingStub.withInterceptors( MetadataUtils.newAttachHeadersInterceptor(
                metadata ) );
    }

    public SnapshotServiceGrpc.SnapshotServiceStub snapshotService()
    {
        final SnapshotServiceGrpc.SnapshotServiceStub snapshotServiceStub = SnapshotServiceGrpc.newStub(
                getChannel() );
        final Metadata metadata = buildMetaData();

        return snapshotServiceStub.withInterceptors( MetadataUtils.newAttachHeadersInterceptor( metadata ) );
    }

    private Metadata buildMetaData()
    {
        final IAuthProvider provider = AuthProvider.provider( this.settings );
        final Map<String, String> headers = provider.provide();

        final Metadata metadata = new Metadata();
        for( Map.Entry<String, String> header : headers.entrySet() )
        {
            metadata.put( Metadata.Key.of( header.getKey(), Metadata.ASCII_STRING_MARSHALLER ), header.getValue() );
        }
        return metadata;
    }
}
