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

package com.intergral.deep.plugins.cf;

import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import java.util.HashMap;

/**
 * This plugin is activated when we are running on an adobe CF server.
 * <p>
 * This plugin will attach the cf version and the cf app name to the captured snapshots.
 */
public class CFPlugin implements IPlugin {

  @Override
  public Resource decorate(final ISettings settings, final ISnapshotContext context) {
    String appName = null;
    try {
      appName = context.evaluateExpression("APPLICATION.applicationname");
    } catch (Throwable ignored) {
      // nothing we can do - so ignore
    }

    final HashMap<String, Object> cfAttributes = new HashMap<>();
    cfAttributes.put("cf_version", Utils.loadCFVersion());
    if (appName != null) {
      cfAttributes.put("app_name", appName);
    }
    return Resource.create(cfAttributes);
  }

  @Override
  public boolean isActive(final ISettings settings) {
    if (!Utils.isCFServer()) {
      return false;
    }
    return IPlugin.super.isActive(settings);
  }
}
