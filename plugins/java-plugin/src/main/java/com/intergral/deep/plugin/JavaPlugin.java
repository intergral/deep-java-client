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

package com.intergral.deep.plugin;

import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.plugin.ISnapshotDecorator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import java.util.HashMap;
import java.util.Map;

/**
 * This plugin captures the thread name of the thread the snapshot was cpatured on.
 */
public class JavaPlugin implements IDeepPlugin, ISnapshotDecorator {

  private final Map<String, String> basic = new HashMap<>();

  public JavaPlugin() {
    this.basic.put("java_version", System.getProperty("java.version"));
  }

  @Override
  public Resource decorate(final ISettings settings, final ISnapshotContext snapshot) {
    final Map<String, Object> javaMap = new HashMap<>(this.basic);
    final Thread thread = Thread.currentThread();

    javaMap.put("thread_name", thread.getName());
    return Resource.create(javaMap);
  }
}
