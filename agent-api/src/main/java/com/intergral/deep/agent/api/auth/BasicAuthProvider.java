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

package com.intergral.deep.agent.api.auth;

import com.intergral.deep.agent.api.settings.ISettings;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

public class BasicAuthProvider implements IAuthProvider {

  private final ISettings settings;

  public BasicAuthProvider(final ISettings settings) {
    this.settings = settings;
  }

  @Override
  public Map<String, String> provide() {
    final String userName = settings.getSettingAs("service.username", String.class);
    final String password = settings.getSettingAs("service.password", String.class);
    if (userName == null || password == null) {
      return Collections.emptyMap();
    }
    final String encodedString = Base64.getEncoder()
        .encodeToString(String.format("%s:%s", userName, password).getBytes());
    return Collections.singletonMap("authorization", "basic%20" + encodedString);
  }
}
