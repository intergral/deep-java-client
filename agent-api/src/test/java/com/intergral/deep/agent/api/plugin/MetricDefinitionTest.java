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
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricDefinitionTest {

  private MetricDefinition metricDefinition;

  @BeforeEach
  void setUp() {
    final HashMap<String, String> tags = new HashMap<>();
    tags.put("one", "tag");
    metricDefinition = new MetricDefinition("metric-name", tags, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit");
  }

  @Test
  void getName() {
    assertEquals("metric-name", metricDefinition.getName());
  }

  @Test
  void getTags() {
    final HashMap<String, String> expectedTags = new HashMap<>();
    expectedTags.put("one", "tag");
    assertEquals(expectedTags, metricDefinition.getTags());
  }

  @Test
  void getType() {
    assertEquals("metric-type", metricDefinition.getType());
  }

  @Test
  void getExpression() {
    assertEquals("metric-expression", metricDefinition.getExpression());
  }

  @Test
  void getNamespace() {
    assertEquals("metric-namespace", metricDefinition.getNamespace());
  }

  @Test
  void getHelp() {
    assertEquals("metric-help", metricDefinition.getHelp());
  }

  @Test
  void getUnit() {
    assertEquals("metric-unit", metricDefinition.getUnit());
  }

  @Test
  void equals() {
    final HashMap<String, String> tags = new HashMap<>();
    tags.put("one", "tag");
    assertEquals(metricDefinition, new MetricDefinition("metric-name", tags, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit"));
    assertNotSame(metricDefinition, new MetricDefinition("metric-name", tags, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit"));

    assertEquals(metricDefinition.hashCode(),
        new MetricDefinition("metric-name", tags, "metric-type", "metric-expression", "metric-namespace",
            "metric-help", "metric-unit").hashCode());

    assertEquals(metricDefinition.toString(),
        new MetricDefinition("metric-name", tags, "metric-type", "metric-expression", "metric-namespace",
            "metric-help", "metric-unit").toString());
  }
}