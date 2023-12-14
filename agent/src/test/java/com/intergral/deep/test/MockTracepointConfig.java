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

package com.intergral.deep.test;

import com.intergral.deep.agent.api.plugin.MetricDefinition;
import com.intergral.deep.agent.types.TracePointConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A mock tracepoint config to make it easier to create during testing.
 */
public class MockTracepointConfig extends TracePointConfig {

  public MockTracepointConfig() {
    super("tp-id", "path", 123, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
  }

  public MockTracepointConfig(final String path) {
    super("tp-id", path, 123, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
  }


  public MockTracepointConfig(final String path, final int line) {
    super("tp-id", path, line, new HashMap<>(), new ArrayList<>(), new ArrayList<>());
  }

  public MockTracepointConfig withArg(final String key, final String value) {
    this.getArgs().put(key, value);
    return this;
  }

  public MockTracepointConfig withWatches(final String... watches) {
    this.getWatches().addAll(Arrays.asList(watches));
    return this;
  }

  public MockTracepointConfig withMetric() {
    this.getMetricDefinitions()
        .add(new MetricDefinition("name", new ArrayList<>(), "COUNTER", "", "deep_agent", "Metric generated from expression: ", "unit"));
    return this;
  }
}
