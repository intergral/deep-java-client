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

import com.intergral.deep.api.IDeepLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.bytebuddy.agent.ByteBuddyAgent;

public class DeepLoader implements IDeepLoader {

  @Override
  public void load(final String pid, final String config) {
    final File agentJar = getAgentJar();
    final File tools = getToolsJar();
    if (agentJar == null) {
      throw new RuntimeException("Cannot find jar.");
    }
    ByteBuddyAgent.attach(agentJar, pid, config, new ShippedToolsJarProvider(tools));
  }


  /**
   * Load the tools jar as a file object. We use the tools jar to dynamically attach to the vm. A
   * version of tools is shipped if we are running a JRE.
   *
   * @return the {@link File} for the tools jar or {@code null}
   */
  private File getToolsJar() {
    final InputStream resourceAsStream = Deep.class.getResourceAsStream("/tools.jar");
    final String pathname = extractLibrary(resourceAsStream);
    if (pathname != null) {
      return new File(pathname);
    }
    return null;
  }


  /**
   * Load the agent jar to a file
   *
   * @return the {@link File} object for the agent
   */
  private File getAgentJar() {
    final InputStream resourceAsStream = getAgentJarStream();
    final String pathname = extractLibrary(resourceAsStream);
    if (pathname != null) {
      return new File(pathname);
    }
    return null;
  }


  /**
   * Log the agent file as a stream
   *
   * @return the stream to use, or {@code null}
   */
  private InputStream getAgentJarStream() {
    // this is pretty much just for testing, see Example
    final String property = System.getProperty("deep.jar.path");
    if (property != null) {
      try {
        return new FileInputStream(property);
      } catch (FileNotFoundException e) {
        System.err.println("Unable to load Deep jar from path: " + property);
        e.printStackTrace(System.err);
        return null;
      }
    }
    return Deep.class.getResourceAsStream("/deep-agent.jar");
  }


  /**
   * Extract a stream to a temp file and return the absolute file path
   *
   * @param inputStream the stream to extract
   * @return the absolute file path to the extracted library
   */
  private String extractLibrary(final InputStream inputStream) {
    if (inputStream == null) {
      return null;
    }

    FileOutputStream fileOutputStream = null;
    try {
      final File tempFile = File.createTempFile("deep", "agent");

      fileOutputStream = new FileOutputStream(tempFile);

      byte[] buf = new byte[1024];

      int len;
      while ((len = inputStream.read(buf)) > 0) {
        fileOutputStream.write(buf, 0, len);
      }

      fileOutputStream.close();
      inputStream.close();
      //noinspection ResultOfMethodCallIgnored
      tempFile.setExecutable(true, true);

      return tempFile.getAbsolutePath();
    } catch (IOException e) {
      return null;
    } finally {
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

}
