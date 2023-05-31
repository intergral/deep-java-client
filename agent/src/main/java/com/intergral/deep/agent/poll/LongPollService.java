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

import com.intergral.deep.agent.Utils;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LongPollService implements ITimerTask
{
    private final Settings settings;
    private final GrpcService grpcService;
    private final DriftAwareThread thread;
    private String currentHash;
    private ITracepointConfig tracepointConfig;

    public LongPollService( final Settings settings, final GrpcService grpcService )
    {
        this.settings = settings;
        this.grpcService = grpcService;
        this.thread = new DriftAwareThread( LongPollService.class.getSimpleName(),
                this,
                settings.getSettingAs( "poll.timer", Integer.class ) );
    }

    public void start( final ITracepointConfig tracepointConfig )
    {
        this.tracepointConfig = tracepointConfig;
        thread.start( 0 );
    }

    @Override
    public void run( long now )
    {
        final PollConfigGrpc.PollConfigBlockingStub blockingStub = this.grpcService.pollService();

        final PollRequest.Builder builder = PollRequest.newBuilder();
        if( this.tracepointConfig.currentHash() != null )
        {
            builder.setCurrentHash( this.tracepointConfig.currentHash() );
        }

        final PollRequest pollRequest = builder
                .setTsNanos( Utils.currentTimeNanos()[1] )
                .setResource( buildResource() )
                .build();

        final PollResponse response = blockingStub.poll( pollRequest );

        if( response.getResponseType() == ResponseType.NO_CHANGE )
        {
            this.tracepointConfig.noChange( response.getTsNanos() );
        }
        else
        {
            this.tracepointConfig.configUpdate( response.getTsNanos(),
                    response.getCurrentHash(),
                    convertResponse( response.getResponseList() ) );
        }
    }

    private Collection<TracePointConfig> convertResponse(
            List<com.intergral.deep.proto.tracepoint.v1.TracePointConfig> responseList )
    {
        return responseList.stream()
                .map( tracePointConfig -> new TracePointConfig( tracePointConfig.getID(),
                        tracePointConfig.getPath(),
                        tracePointConfig.getLineNumber(),
                        Collections.unmodifiableMap( new HashMap<>( tracePointConfig.getArgsMap() ) ),
                        Collections.unmodifiableCollection( tracePointConfig.getWatchesList() ) ) )
                .collect( Collectors.toList() );
    }

    private com.intergral.deep.proto.resource.v1.Resource buildResource()
    {
        final Resource resource = this.settings.getResource();
        return com.intergral.deep.proto.resource.v1.Resource.newBuilder()
                .addAllAttributes( resource.getAttributes().entrySet().stream().map( entry -> KeyValue.newBuilder()
                        .setKey( entry.getKey() )
                        .setValue( AnyValue.newBuilder().setStringValue(
                                String.valueOf( entry.getValue() ) ).build() )
                        .build() ).collect( Collectors.toList() ) )
                .build();
    }

    @Override
    public long callback( long duration, long nextExecutionTime )
    {
        return nextExecutionTime;
    }
}
