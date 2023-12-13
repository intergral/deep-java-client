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

import com.intergral.deep.agent.api.settings.ISettings;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BasicAuthProviderTest {

  @Test
  void canProvide() {
    final ISettings settings = Mockito.mock(ISettings.class);
    final BasicAuthProvider basicAuthProvider = new BasicAuthProvider();
    basicAuthProvider.configure(settings, null);

    assertEquals(0, basicAuthProvider.provide().size());
  }

  @Test
  void canProvide_settings() {
    final ISettings settings = Mockito.mock(ISettings.class);
    Mockito.doReturn("username").doReturn("password").when(settings).getSettingAs(Mockito.anyString(), Mockito.any());
    final BasicAuthProvider basicAuthProvider = new BasicAuthProvider();
    basicAuthProvider.configure(settings, null);

    final Map<String, String> provide = basicAuthProvider.provide();
    assertEquals(1, provide.size());
    assertEquals("basic%20dXNlcm5hbWU6cGFzc3dvcmQ=", provide.get("authorization"));
  }

  @Test
  void canProvide_missingUsername() {
    final ISettings settings = Mockito.mock(ISettings.class);
    Mockito.doReturn(null).doReturn("password").when(settings).getSettingAs(Mockito.anyString(), Mockito.any());
    final BasicAuthProvider basicAuthProvider = new BasicAuthProvider();
    basicAuthProvider.configure(settings, null);

    final Map<String, String> provide = basicAuthProvider.provide();
    assertEquals(0, provide.size());
  }

  @Test
  void canProvide_missingPassword() {
    final ISettings settings = Mockito.mock(ISettings.class);
    Mockito.doReturn("username").doReturn(null).when(settings).getSettingAs(Mockito.anyString(), Mockito.any());
    final BasicAuthProvider basicAuthProvider = new BasicAuthProvider();
    basicAuthProvider.configure(settings, null);

    final Map<String, String> provide = basicAuthProvider.provide();
    assertEquals(0, provide.size());
  }
}