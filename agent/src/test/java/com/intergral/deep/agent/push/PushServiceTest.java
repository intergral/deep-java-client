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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.push.PushService.LoggingObserver;
import com.intergral.deep.proto.tracepoint.v1.SnapshotServiceGrpc.SnapshotServiceStub;
import com.intergral.deep.test.MockEventSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PushServiceTest {

  private PushService pushService;
  private SnapshotServiceStub snapshotServiceStub;

  @BeforeEach
  void setUp() {
    final GrpcService grpcService = Mockito.mock(GrpcService.class);
    snapshotServiceStub = Mockito.mock(SnapshotServiceStub.class);
    when(grpcService.snapshotService()).thenReturn(snapshotServiceStub);

    pushService = new PushService(grpcService);
  }

  @Test
  void snapshotLogger() {
    final LoggingObserver observer = new LoggingObserver("test id");
    assertDoesNotThrow(() -> observer.onError(new RuntimeException("test exception")));
    assertDoesNotThrow(() -> observer.onNext(null));
    assertDoesNotThrow(observer::onCompleted);
  }

  @Test
  void canPushSnapshot() {
    pushService.pushSnapshot(new MockEventSnapshot());

    verify(snapshotServiceStub).send(any(), any());
  }
}