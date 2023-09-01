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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * This is an implementation of the {@link net.bytebuddy.agent.ByteBuddyAgent.AttachmentProvider}
 * that uses the tools jar that we ship with the agent.
 */
public class ShippedToolsJarProvider implements ByteBuddyAgent.AttachmentProvider {

  private final File tools;


  /**
   * Create a new provider.
   *
   * @param tools the file object for the tools jar to use
   */
  public ShippedToolsJarProvider(final File tools) {
    this.tools = tools;
  }


  @Override
  public Accessor attempt() {
    // first just try the normal default loaders
    final Accessor attempt = DEFAULT.attempt();
    if (attempt.isAvailable()) {
      return attempt;
    }

    // if the tools jar is not available then return a failure.
    if (tools == null) {
      return Accessor.Unavailable.INSTANCE;
    } else {
      // else load the tools jar and return an accessor to use it
      try {
        return Accessor.Simple.of(new URLClassLoader(new URL[]{tools.toURI().toURL()}, null),
            tools);
      } catch (MalformedURLException exception) {
        throw new IllegalStateException("Could not represent " + tools + " as URL");
      }
    }
  }
}
