package com.intergral.deep;

import com.intergral.deep.proto.poll.v1.PollConfigGrpc;
import com.intergral.deep.proto.poll.v1.PollRequest;
import com.intergral.deep.proto.poll.v1.PollResponse;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        NettyChannelBuilder ncBuilder = NettyChannelBuilder.forAddress("localhost", 43315)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .enableRetry()
                .executor(Executors.newFixedThreadPool(1))
                .maxRetryAttempts(Integer.MAX_VALUE);

//        // Select secure or not
//        if( grpcPort == 443 )
//        {
//            ncBuilder.useTransportSecurity();
//        }
//        else
//        {
        ncBuilder.usePlaintext();
//        }

        final ManagedChannel build = ncBuilder.build();

        final PollConfigGrpc.PollConfigStub pollConfigStub = PollConfigGrpc.newStub(build);

        pollConfigStub.poll(PollRequest.newBuilder().build(), new StreamObserver<PollResponse>() {
            @Override
            public void onNext(PollResponse pollResponse) {
                System.out.println(pollResponse);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                System.out.println(throwable);
            }

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
            }
        });

        Thread.sleep(5000);
    }
}