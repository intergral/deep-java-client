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

package com.intergral.deep.agent.tracepoint.inst;

/**
 * Utilities to help with instrumentation.
 */
public final class InstUtils {

  private InstUtils() {
  }

  /**
   * Get the external class name.
   *
   * @param className the class name
   * @return the external class name
   */
  public static String externalClassName(final String className) {
    return className.replaceAll("/", ".");
  }


  /**
   * Get the name of the file from the path.
   *
   * @param relPath the path to a file
   * @return the name of the file
   */
  public static String fileName(final String relPath) {
    final int lastIndexOf = relPath.lastIndexOf('/');
    if (lastIndexOf == -1) {
      return relPath;
    }
    return relPath.substring(lastIndexOf + 1);
  }


  /**
   * Convert the class name to the internal class name, remove any inner class names.
   *
   * @param clazz the class
   * @return the internal class name without the inner classes
   */
  public static String internalClassStripInner(final Class<?> clazz) {
    return internalClassStripInner(clazz.getName());
  }


  /**
   * Convert the class name to the internal class name, remove any inner class names.
   *
   * @param className the name of the class
   * @return the internal class name without the inner classes
   */
  public static String internalClassStripInner(final String className) {
    final int index = className.indexOf('$');
    if (index == -1) {
      return internalClass(className);
    }
    return internalClass(className.substring(0, index));
  }

  /**
   * Get the internal class name.
   *
   * @param clazz the name of the class
   * @return the internal name of the class
   */
  public static String internalClass(final String clazz) {
    return clazz.replaceAll("\\.", "/");
  }

  /**
   * Get the internal class name.
   *
   * @param clazz the class
   * @return the internal name of the class
   */
  public static String internalClass(final Class<?> clazz) {
    return internalClass(clazz.getName());
  }

  /**
   * Get the short version of the class name.
   * <p>
   * Sometimes {@link Class#getSimpleName()} doesn't return a name. So we need one that always returns a name.
   *
   * @param className the class name
   * @return the name of the class without the package
   */
  public static String shortClassName(final String className) {
    return className.substring(className.lastIndexOf('.') + 1);
  }
}
