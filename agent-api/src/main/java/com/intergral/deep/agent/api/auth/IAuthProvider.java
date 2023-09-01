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

import java.util.Map;

/**
 * Allows for custom auth providers to be configured. These can be provided as an instantiatable class using the class name via the setting
 * {@code service.auth.provider}. Alternatively a plugin can be configured as an auth provider.
 */
public interface IAuthProvider {

  /**
   * Provide the headers that should be attached to the GRPC calls.
   *
   * @return a Map of the header values
   */
  Map<String, String> provide();
}
