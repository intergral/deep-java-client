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

import java.util.Map;
import java.util.Objects;

/**
 * This type defines a metric that is attached to a tracepoint.
 */
public class MetricDefinition {

  private final String name;
  private final Map<String, String> tags;
  private final String type;
  private final String expression;
  private final String namespace;
  private final String help;
  private final String unit;

  /**
   * Create a new MetricDefinition.
   *
   * @param name       the name of the metric
   * @param tags       the tags attached to the metric
   * @param type       the type of the metric
   * @param expression the expression used to calculate the value for this metric
   * @param namespace  the namespace the metric should be in
   * @param help       the help statement for the metric
   * @param unit       the unit for the metric
   */
  public MetricDefinition(
      final String name,
      final Map<String, String> tags,
      final String type,
      final String expression,
      final String namespace,
      final String help,
      final String unit) {

    this.name = name;
    this.tags = tags;
    this.type = type;
    this.expression = expression;
    this.namespace = namespace;
    this.help = help;
    this.unit = unit;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public String getType() {
    return type;
  }

  public String getExpression() {
    return expression;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getHelp() {
    return help;
  }

  public String getUnit() {
    return unit;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final MetricDefinition that = (MetricDefinition) o;
    return Objects.equals(name, that.name) && Objects.equals(tags, that.tags) && Objects.equals(type, that.type)
        && Objects.equals(expression, that.expression) && Objects.equals(namespace, that.namespace)
        && Objects.equals(help, that.help) && Objects.equals(unit, that.unit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, tags, type, expression, namespace, help, unit);
  }

  @Override
  public String toString() {
    return "MetricDefinition{"
        + "name='" + name + '\''
        + ", tags=" + tags
        + ", type='" + type + '\''
        + ", expression='" + expression + '\''
        + ", namespace='" + namespace + '\''
        + ", help='" + help + '\''
        + ", unit='" + unit + '\''
        + '}';
  }
}
