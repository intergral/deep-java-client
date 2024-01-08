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

package com.intergral.deep.agent.push;

import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service deals with pushing the collected data to the remote services.
 */
public class PushService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushService.class);
  private final GrpcService grpcService;


  public PushService(final GrpcService grpcService) {
    this.grpcService = grpcService;
  }

  /**
   * Decorate and push the provided snapshot.
   *
   * @param snapshot the snapshot to push
   */
  public void pushSnapshot(final EventSnapshot snapshot) {
    final Snapshot grpcSnapshot = PushUtils.convertToGrpc(snapshot);
    this.grpcService.snapshotService().send(grpcSnapshot, new LoggingObserver(snapshot.getID()));
  }

  static class LoggingObserver implements StreamObserver<SnapshotResponse> {

    private final String id;

    public LoggingObserver(final String id) {
      this.id = id;
    }

    @Override
    public void onNext(final SnapshotResponse value) {
      LOGGER.debug("Sent snapshot: {}", id);

    }

    @Override
    public void onError(final Throwable t) {
      LOGGER.error("Error sending snapshot: {}", id, t);
    }

    @Override
    public void onCompleted() {

    }
  }
}
