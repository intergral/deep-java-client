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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.intergral.deep.agent.api.plugin.MetricDefinition.Label;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MetricDefinitionTest {

  private MetricDefinition metricDefinition;

  @BeforeEach
  void setUp() {
    final ArrayList<Label> labels = new ArrayList<>();
    labels.add(new Label("key", "value", null));
    metricDefinition = new MetricDefinition("metric-name", labels, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit");
  }

  @Test
  void getName() {
    assertEquals("metric-name", metricDefinition.getName());
  }

  @Test
  void getLabels() {
    final ArrayList<Label> expectedLabels = new ArrayList<>();
    expectedLabels.add(new Label("key", "value", null));
    assertEquals(expectedLabels, metricDefinition.getLabels());
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
    final ArrayList<Label> labels = new ArrayList<>();
    labels.add(new Label("key", "value", null));
    assertEquals(metricDefinition, new MetricDefinition("metric-name", labels, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit"));
    assertNotSame(metricDefinition, new MetricDefinition("metric-name", labels, "metric-type", "metric-expression", "metric-namespace",
        "metric-help", "metric-unit"));

    assertEquals(metricDefinition.hashCode(),
        new MetricDefinition("metric-name", labels, "metric-type", "metric-expression", "metric-namespace",
            "metric-help", "metric-unit").hashCode());

    assertEquals(metricDefinition.toString(),
        new MetricDefinition("metric-name", labels, "metric-type", "metric-expression", "metric-namespace",
            "metric-help", "metric-unit").toString());
  }

  @Nested
  class LabelTest {

    private Label label;

    @BeforeEach
    void setUp() {
      label = new Label("key", "value", null);
    }

    @Test
    void getKey() {
      assertEquals("key", label.getKey());
    }

    @Test
    void getValue() {
      assertEquals("value", label.getValue());
    }

    @Test
    void getExpression() {
      assertNull(label.getExpression());
    }
  }
}