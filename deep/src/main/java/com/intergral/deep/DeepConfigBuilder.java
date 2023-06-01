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

package com.intergral.deep;

import java.util.HashMap;
import java.util.Map;

public class DeepConfigBuilder {

  private final Map<String, Object> config = new HashMap<>();

  public void start() {
    Deep.getInstance().startWithConfig(this.build());
  }

  public DeepConfigBuilder setValue(final String key, final Object value) {
    this.config.put(key, value);
    return this;
  }

  private String build() {
    return this.configAsArgs(this.config);
  }

  /**
   * Convert the config to a string
   *
   * @param config the config to parse
   * @return the config as a string
   */
  String configAsArgs(final Map<String, Object> config) {
    final StringBuilder stringBuilder = new StringBuilder();
    for (Map.Entry<String, Object> entry : config.entrySet()) {
      if (entry.getValue() == null) {
        continue;
      }
      if (stringBuilder.length() != 0) {
        stringBuilder.append(',');
      }
      if (entry.getValue() instanceof Map) {
        //noinspection unchecked
        stringBuilder.append(entry.getKey())
            .append('=')
            .append(configAsArgs((Map<String, Object>) entry.getValue()));
      } else {
        stringBuilder.append(entry.getKey()).append('=').append(entry.getValue());
      }
    }
    return stringBuilder.toString();
  }

}
