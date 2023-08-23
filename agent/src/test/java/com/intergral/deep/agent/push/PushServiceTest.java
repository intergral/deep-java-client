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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.grpc.GrpcService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.SnapshotServiceGrpc.SnapshotServiceStub;
import com.intergral.deep.test.MockEventSnapshot;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PushServiceTest {

  private PushService pushService;
  private SnapshotServiceStub snapshotServiceStub;
  private Settings settings;

  @BeforeEach
  void setUp() {
    final GrpcService grpcService = Mockito.mock(GrpcService.class);
    snapshotServiceStub = Mockito.mock(SnapshotServiceStub.class);
    when(grpcService.snapshotService()).thenReturn(snapshotServiceStub);
    final HashMap<String, String> map = new HashMap<>();

    settings = Settings.build(map);
    pushService = new PushService(settings, grpcService);
  }

  @Test
  void canPushSnapshot() {
    final ISnapshotContext context = Mockito.mock(ISnapshotContext.class);
    pushService.pushSnapshot(new MockEventSnapshot(), context);

    verify(snapshotServiceStub).send(any(), any());
  }

  @Test
  void doesDecorate() {
    settings.addPlugin(new TestDecorator());

    final ISnapshotContext context = Mockito.mock(ISnapshotContext.class);
    pushService.pushSnapshot(new MockEventSnapshot(), context);

    final ArgumentCaptor<Snapshot> argumentCaptor = ArgumentCaptor.forClass(Snapshot.class);
    verify(snapshotServiceStub).send(argumentCaptor.capture(), any());

    final Snapshot value = argumentCaptor.getValue();

    assertNotNull(value);

    final KeyValue attributes = value.getAttributes(0);
    assertEquals("decorated", attributes.getKey());
    assertEquals("value", attributes.getValue().getStringValue());
  }

  public static class TestDecorator implements IPlugin {

    @Override
    public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
      return Resource.create(Collections.singletonMap("decorated", "value"));
    }
  }
}