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

package com.intergral.deep.agent.tracepoint.inst.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Utilities for transforming the classes.
 */
public final class TransformerUtils {

  private TransformerUtils() {
  }

  private static final List<String> EXCLUDE_PACKAGES = Collections.emptyList();
  private static final List<String> EXCLUDE_CONTAINS = Collections.emptyList();


  /**
   * Store the transformed bytes to allow us to debug them in enabled.
   *
   * @param path        the path to store the bytes
   * @param original    the unmodified bytes
   * @param transformed the modified bytes
   * @param className   the class name being modified
   * @return {@code true} if we stored the data
   */
  public static boolean storeUnsafe(String path, byte[] original, byte[] transformed,
      String className) {
    try {
      if (path == null || path.isEmpty()) {
        return false;
      }
      store(path, original, transformed, className);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }


  static void store(String path, byte[] originalBytes, byte[] transformedBytes,
      String className)
      throws Exception {
    if (path == null) {
      return;
    }
    File dir = new File(path);
    dir.mkdirs();
    dir.setReadable(true, false);
    dir.setExecutable(true, false);
    dir.setWritable(true, false);

    String filename = className.replace("/", "_");

    writeToFile(originalBytes, dir, filename + ".class");

    writeToFile(transformedBytes, dir, filename + "_trnfm.class");
  }

  private static void writeToFile(final byte[] originalBytes, final File dir, final String filename) throws IOException {
    File org = new File(dir, filename);
    if (org.exists()) {
      org.delete();
    }
    FileOutputStream out = new FileOutputStream(org, false);
    out.write(originalBytes);
    out.close();

    org.setReadable(true, false);
    org.setWritable(true, false);
    org.setExecutable(true, false);
  }


  /**
   * Is the class excluded from transformation.
   *
   * @param clazz the class
   * @return {@code true} if we should not transform this class.
   */
  public static boolean isExcludedClass(final Class<?> clazz) {
    return isExcludedClass(clazz.getName());
  }


  /**
   * Is the class excluded from transformation.
   *
   * @param classname the name of the class
   * @return {@code true} if we should not transform this class.
   */
  public static boolean isExcludedClass(final String classname) {
    for (final String pkg : EXCLUDE_PACKAGES) {
      if (classname.startsWith(pkg)) {
        return true;
      }
    }

    for (final String contain : EXCLUDE_CONTAINS) {
      if (classname.contains(contain)) {
        return true;
      }
    }

    return false;
  }
}
