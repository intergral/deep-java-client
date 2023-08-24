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

package com.intergral.deep.agent.api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.intergral.deep.agent.api.auth.AuthProvider.NoopProvider;
import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthProviderTest {

  @Test
  void coverage() {
    // for coverage
    //noinspection ObviousNullCheck,InstantiationOfUtilityClass
    assertNotNull(new AuthProvider());
  }

  @Test
  void canProvide() {
    final ISettings settings = Mockito.mock(ISettings.class);
    final IReflection reflection = Mockito.mock(IReflection.class);
    final IAuthProvider provider = AuthProvider.provider(settings, reflection);

    assertEquals(provider.getClass(), NoopProvider.class);
  }

  @Test
  void canLoadProviderByName() {
    final ISettings settings = Mockito.mock(ISettings.class);
    Mockito.doReturn(MockAuthProviderPlugin.class.getName()).when(settings).getSettingAs("service.auth.provider", String.class);
    final IReflection reflection = Mockito.mock(IReflection.class);
    Mockito.when(reflection.findConstructor(Mockito.any(), Mockito.any()))
        .thenAnswer(invocationOnMock -> MockAuthProviderPlugin.class.getConstructor());
    Mockito.when(reflection.callConstructor(Mockito.any())).thenReturn(new MockAuthProviderPlugin());
    final IAuthProvider provider = AuthProvider.provider(settings, reflection);

    assertEquals(MockAuthProviderPlugin.class, provider.getClass());
  }

  @Test
  void willUseNoopProvider() {
    final ISettings settings = Mockito.mock(ISettings.class);
    final IReflection reflection = Mockito.mock(IReflection.class);
    final IAuthProvider provider = AuthProvider.provider(settings, reflection);
    assertEquals(NoopProvider.class, provider.getClass());
    assertEquals(Collections.emptyMap(), provider.provide());
  }

  public static class MockAuthProviderPlugin implements IPlugin, IAuthProvider {

    @Override
    public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
      return Resource.create(Collections.singletonMap("test", "provider"));
    }

    @Override
    public Map<String, String> provide() {
      return Collections.singletonMap("test", "provider");
    }
  }
}