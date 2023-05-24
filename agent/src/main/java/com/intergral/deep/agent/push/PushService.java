package com.intergral.deep.agent.push;

import com.google.common.util.concurrent.ListenableFuture;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PushService
{
    private static final Logger LOGGER = LoggerFactory.getLogger( PushService.class );
    private final Settings settings;
    private final GrpcService grpcService;


    public PushService( final Settings settings, final GrpcService grpcService )
    {
        this.settings = settings;
        this.grpcService = grpcService;
    }

    public void pushSnapshot( final EventSnapshot snapshot )
    {
        final Snapshot grpcSnapshot = PushUtils.convertToGrpc( snapshot );
        this.grpcService.snapshotService().send( grpcSnapshot, new StreamObserver<SnapshotResponse>()
        {
            @Override
            public void onNext( final SnapshotResponse value )
            {
                LOGGER.debug( "Sent snapshot: {}", snapshot.getID() );
            }

            @Override
            public void onError( final Throwable t )
            {
                LOGGER.error( "Error sending snapshot: {}", snapshot.getID(), t );
            }

            @Override
            public void onCompleted()
            {

            }
        } );
    }
}
