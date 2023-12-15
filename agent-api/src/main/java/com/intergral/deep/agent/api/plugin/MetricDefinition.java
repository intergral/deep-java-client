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

import java.util.List;
import java.util.Objects;

/**
 * This type defines a metric that is attached to a tracepoint.
 */
public class MetricDefinition {

  private final String name;
  private final List<Label> labels;
  private final String type;
  private final String expression;
  private final String namespace;
  private final String help;
  private final String unit;

  /**
   * Create a new MetricDefinition.
   *
   * @param name       the name of the metric
   * @param labels     the labels attached to the metric
   * @param type       the type of the metric
   * @param expression the expression used to calculate the value for this metric
   * @param namespace  the namespace the metric should be in
   * @param help       the help statement for the metric
   * @param unit       the unit for the metric
   */
  public MetricDefinition(
      final String name,
      final List<Label> labels,
      final String type,
      final String expression,
      final String namespace,
      final String help,
      final String unit) {

    this.name = name;
    this.labels = labels;
    this.type = type;
    this.expression = expression;
    this.namespace = namespace;
    this.help = help;
    this.unit = unit;
  }

  public String getName() {
    return name;
  }

  public List<Label> getLabels() {
    return labels;
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
    return Objects.equals(name, that.name) && Objects.equals(labels, that.labels) && Objects.equals(type, that.type)
        && Objects.equals(expression, that.expression) && Objects.equals(namespace, that.namespace)
        && Objects.equals(help, that.help) && Objects.equals(unit, that.unit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, labels, type, expression, namespace, help, unit);
  }

  @Override
  public String toString() {
    return "MetricDefinition{"
        + "name='" + name + '\''
        + ", tags=" + labels
        + ", type='" + type + '\''
        + ", expression='" + expression + '\''
        + ", namespace='" + namespace + '\''
        + ", help='" + help + '\''
        + ", unit='" + unit + '\''
        + '}';
  }

  /**
   * This type is used to represent a label that is attached to a metric.
   * <p>
   * Labels can have either a static value or an expression. If the value is an expression then this is evaluated as a watcher and the
   * result is used as the label value.
   */
  public static class Label {

    final String key;
    final Object value;
    final String expression;

    /**
     * Create a new label for a metric.
     *
     * @param key        the label key
     * @param value      the label value if a fixed value
     * @param expression the label expression if we should evaluate the label value
     */
    public Label(final String key, final Object value, final String expression) {
      this.key = key;
      this.value = value;
      this.expression = expression;
    }

    public String getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public String getExpression() {
      return expression;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final Label label = (Label) o;
      return Objects.equals(key, label.key) && Objects.equals(value, label.value) && Objects.equals(expression,
          label.expression);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, value, expression);
    }

    @Override
    public String toString() {
      return "Label{"
          + "key='" + key + '\''
          + ", value=" + value
          + ", expression='" + expression + '\''
          + '}';
    }
  }
}
