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

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.ResourceProvider;
import java.util.Collections;

/**
 * A resource provider that detects the hava version to add to the resource.
 */
public class JavaResourceDetector implements ResourceProvider {

  @Override
  public Resource createResource(final ISettings settings) {
    final String property = System.getProperty("java.version");
    if (property == null) {
      return null;
    }
    return Resource.create(Collections.singletonMap("java_version", property));
  }
}
