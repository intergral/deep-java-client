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

package com.intergral.deep.agent.resource;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intergral.deep.agent.api.spi.ResourceProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpiUtilTest {

  @Test
  void loadsOrderedSpi() {
    ResourceProvider spi1 = mock(ResourceProvider.class);
    ResourceProvider spi2 = mock(ResourceProvider.class);
    ResourceProvider spi3 = mock(ResourceProvider.class);

    when(spi1.order()).thenReturn(2);
    when(spi2.order()).thenReturn(0);
    when(spi3.order()).thenReturn(1);

    SpiUtil.ServiceLoaderFinder mockFinder = mock(SpiUtil.ServiceLoaderFinder.class);
    when(mockFinder.load(ResourceProvider.class, SpiUtil.class.getClassLoader()))
        .thenReturn(asList(spi1, spi2, spi3));

    List<ResourceProvider> loadedSpi = SpiUtil.loadOrdered(ResourceProvider.class, getClass().getClassLoader(), mockFinder);

    assertEquals(3, loadedSpi.size());
  }
}