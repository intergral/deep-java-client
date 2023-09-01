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

package com.intergral.deep.agent.api.resource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ResourceTest {

  @Test
  void canCreateDefault() {
    assertNotNull(Resource.DEFAULT);
    assertNotNull(Resource.DEFAULT.getAttributes().get(ResourceAttributes.TELEMETRY_SDK_NAME));
    assertNotNull(Resource.DEFAULT.getAttributes().get(ResourceAttributes.TELEMETRY_SDK_LANGUAGE));
    assertNotNull(Resource.DEFAULT.getAttributes().get(ResourceAttributes.TELEMETRY_SDK_VERSION));
    assertNull(Resource.DEFAULT.getSchemaUrl());
  }

  @Test
  void canMerge() {
    assertNotNull(Resource.DEFAULT.merge(null));
    assertNotNull(Resource.DEFAULT.merge(Resource.create(new HashMap<>())));

    final HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("merged", "thing");
    attributes.put(ResourceAttributes.TELEMETRY_SDK_VERSION, "test_override");
    final Resource merged = Resource.DEFAULT.merge(Resource.create(attributes));
    assertEquals("thing", merged.getAttributes().get("merged"));
    assertEquals("test_override", merged.getAttributes().get(ResourceAttributes.TELEMETRY_SDK_VERSION));
  }
}