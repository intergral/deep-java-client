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

package com.intergral.deep.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * This is the main entry point for the Deep agent.
 */
public final class Agent {

  private Agent() {
  }

  /**
   * This is called when the agent is dynamically attached to the VM.
   *
   * @param arg  the agent args
   * @param inst a system instrumentation
   */
  public static void agentmain(final String arg, final Instrumentation inst) {
    final Map<String, String> args = parseArgs(arg);
    args.put("deep.start_method", "agentmain");
    startNv(args, inst);
  }


  /**
   * This is called when the agent is attached from the CLI.
   *
   * @param arg  the agent args
   * @param inst a system instrumentation
   */
  public static void premain(final String arg, final Instrumentation inst) {
    final Map<String, String> args = parseArgs(arg);
    args.put("deep.start_method", "premain");
    startNv(args, inst);
  }

  /**
   * A common start for NV.
   *
   * @param args the NV args
   * @param inst a system instrumentation
   */
  private static void startNv(final Map<String, String> args, final Instrumentation inst) {
    try {
      final JarFile jarfile;
      // this is here, so we can set the path during testing.
      if (args.containsKey("deep.path")) {
        jarfile = new JarFile(new File(args.get("deep.path")));
      } else {
        final URL location = Agent.class.getProtectionDomain().getCodeSource().getLocation();
        jarfile = new JarFile(location.getFile());
      }

      inst.appendToBootstrapClassLoaderSearch(jarfile);

      final Class<?> c = Class.forName("com.intergral.deep.agent.AgentImpl", true, null);
      final Method method = c.getMethod("startup", Instrumentation.class, Map.class);
      method.invoke(null, inst, args);
    } catch (Throwable t) {
      System.err.println("----------------------------------------------------------");
      System.err.println(
          "ERROR: Failed to start deep agent: " + t.getClass().getName() + " " + t.getMessage());
      System.err.println("----------------------------------------------------------");
      //noinspection CallToPrintStackTrace
      t.printStackTrace();
      System.err.println("----------------------------------------------------------");
    }
  }

  static Map<String, String> parseArgs(final String args) {
    if (args == null) {
      return new HashMap<>();
    }

    final HashMap<String, String> arguments = new HashMap<>();

    final List<String> fullSets = splitArgs(args.trim());
    for (final String set : fullSets) {
      final int idx = set.indexOf('=');
      if (idx != -1) {
        final String first = set.substring(0, idx);
        final String second = set.substring(idx + 1);

        arguments.put(first, second);
      }
    }

    return arguments;
  }

  private static List<String> splitArgs(final String args) {
    final List<String> rtn = new ArrayList<>();

    boolean escaped = false;
    StringBuilder str = new StringBuilder();

    for (int idx = 0; idx < args.length(); idx++) {
      char chr = args.charAt(idx);
      if (chr == '\\') {
        if (!escaped) {
          escaped = true;
        } else {
          str.append("\\\\");
          escaped = false;
        }
        continue;
      }
      if (chr == ',' && !escaped) {
        rtn.add(str.toString());
        str = new StringBuilder();
      } else if (chr == ',') {
        str.append(chr);
      } else {
        if (escaped) {
          str.append("\\");
        }
        str.append(chr);
      }
      escaped = false;
    }

    if (str.length() > 0) {
      rtn.add(str.toString());
    }

    return rtn;
  }
}
