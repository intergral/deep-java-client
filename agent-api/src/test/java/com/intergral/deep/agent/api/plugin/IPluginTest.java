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

package com.intergral.deep.agent.api.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IPluginTest {

  @Test
  void name() {
    assertEquals("com.intergral.deep.agent.api.plugin.IPluginTest$MockPlugin", new MockPlugin().name());
  }

  @Test
  void isActive() {
    final ISettings settings = Mockito.mock(ISettings.class);
    Mockito.when(settings.getSettingAs("com.intergral.deep.agent.api.plugin.IPluginTest$MockPlugin.active", Boolean.class)).thenReturn(null)
        .thenReturn(true).thenReturn(false);
    assertTrue(new MockPlugin().isActive(settings));
    assertTrue(new MockPlugin().isActive(settings));
    assertFalse(new MockPlugin().isActive(settings));
  }

  static class MockPlugin implements IPlugin {

    @Override
    public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
      return null;
    }
  }
}