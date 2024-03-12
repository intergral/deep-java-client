/*
 *     Copyright (C) 2024  Intergral GmbH
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

package com.intergral.deep.agent.plugins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PluginSpiLoaderTest {

  @Test
  void loadPlugins() {
    final ISettings settings = Mockito.mock(ISettings.class);
    final List<IDeepPlugin> iDeepPlugins = PluginSpiLoader.loadPlugins(settings, null, null);
    assertNotNull(iDeepPlugins);

    assertTrue(iDeepPlugins.size() > 1);

    final Set<String> classNames = iDeepPlugins.stream().map(Object::getClass).map(Class::getName).collect(Collectors.toSet());

    assertTrue(classNames.contains(MockPlugin.class.getName()));
    assertFalse(classNames.contains(MockBadPlugin.class.getName()));
  }
}