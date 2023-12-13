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

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.spi.IDeepPlugin;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * This is an {@link IAuthProvider} that will attach basic authorization to the outbound requests.
 * <p>
 * This provider can be set using {@code service.auth.provider=com.intergral.deep.agent.api.auth.BasicAuthProvider}. The  username and
 * password that is configured onto requests can be set with the setting:
 * <ul>
 *   <li>{@code service.username=yourusername}</li>
 *   <li>{@code service.password=yourpassword}</li>
 * </ul>
 * <p>
 * These values are then base64 encoded and attached to the outbound requests as the {@code authorization} header.
 */
public class BasicAuthProvider implements IDeepPlugin, IAuthProvider {

  private ISettings settings;

  @Override
  public IDeepPlugin configure(final ISettings settings, final IReflection reflection) {
    this.settings = settings;
    return this;
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
