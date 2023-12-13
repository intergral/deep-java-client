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

import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.plugin.ISnapshotDecorator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotResponse;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service deals with pushing the collected data to the remote services.
 */
public class PushService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PushService.class);
  private final Settings settings;
  private final GrpcService grpcService;


  public PushService(final Settings settings, final GrpcService grpcService) {
    this.settings = settings;
    this.grpcService = grpcService;
  }

  /**
   * Decorate and push the provided snapshot.
   *
   * @param snapshot the snapshot to push
   * @param context  the context of where the snapshot is collected
   */
  public void pushSnapshot(final EventSnapshot snapshot, final ISnapshotContext context) {
    decorate(snapshot, context);
    final Snapshot grpcSnapshot = PushUtils.convertToGrpc(snapshot);
    this.grpcService.snapshotService().send(grpcSnapshot, new LoggingObserver(snapshot.getID()));
  }

  private void decorate(final EventSnapshot snapshot, final ISnapshotContext context) {
    final Collection<IDeepPlugin> plugins = this.settings.getPlugins();
    for (IDeepPlugin plugin : plugins) {
      if (plugin instanceof ISnapshotDecorator) {
        try {
          final Resource decorate = ((ISnapshotDecorator) plugin).decorate(this.settings, context);
          if (decorate != null) {
            snapshot.mergeAttributes(decorate);
          }
        } catch (Throwable t) {
          LOGGER.error("Error processing plugin {}", plugin.getClass().getName());
        }
      }
    }
    snapshot.close();
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
