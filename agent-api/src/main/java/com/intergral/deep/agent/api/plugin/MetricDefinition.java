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

public class MetricDefinition {

  private final String name;
  private final Map<String, String> tags;
  private final String type;
  private final String expression;
  private final String namespace;
  private final String help;

  public MetricDefinition(
      final String name,
      final Map<String, String> tags,
      final String type,
      final String expression,
      final String namespace,
      final String help) {

    this.name = name;
    this.tags = tags;
    this.type = type;
    this.expression = expression;
    this.namespace = namespace;
    this.help = help;
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
}
