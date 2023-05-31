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

package com.intergral.deep.tests.it.java;

import com.intergral.deep.proto.poll.v1.PollResponse;
import com.intergral.deep.proto.poll.v1.ResponseType;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.TracePointConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeepITTest extends ANVITTest
{
    @Test
    void checkBPFires() throws Exception
    {
        final PollResponse build = PollResponse.newBuilder()
                .setCurrentHash( "" )
                .setResponseType( ResponseType.UPDATE )
                .addResponse( TracePointConfig.newBuilder()
                        .setPath( "BPTestTarget.java" ).setLineNumber( 32 ).build() )
                .build();
        onNext( build );

        grpcConnectLatch.await( 1, TimeUnit.MINUTES );
        final AtomicBoolean hasTriggered = new AtomicBoolean( false );

        new Thread()
        {
            @Override
            public void run()
            {
                while( !hasTriggered.get() )
                {
                    // if this line changes then the test will need changed below
                    final BPTestTarget checkBPFires = new BPTestTarget( "checkBPFires" );
                    System.out.println( checkBPFires.getName() );
                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch( InterruptedException e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        snapshotlatch.await(5, TimeUnit.MINUTES);

        final Snapshot snapshot = snapshotAtomicReference.get();
        assertEquals(snapshot.getVarLookupMap().get( "1" ).getChildren( 0 ).getName(), "name");
        assertEquals(snapshot.getVarLookupMap().get( "2" ).getValue(), "checkBPFires");
    }
}
