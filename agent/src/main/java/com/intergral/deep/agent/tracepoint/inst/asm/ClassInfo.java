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


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassInfo.class);

  private static final Set<String> SAFE_LOADERS = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList("lucee.commons.lang.PCLBlock", "railo.commons.lang.PCLBlock")));
  private static final String LUCEE_LOADER = "lucee.loader.classloader.LuceeClassLoader";
  private static final String RAILO_LOADER = "railo.loader.classloader.RailoClassLoader";

  private final Type type;
  private final ClassReader classReader;
  private final ClassLoader loader;


  public ClassInfo(final String type, final ClassLoader loader, final ClassReader classReader) {
    this.loader = loader;
    this.type = Type.getObjectType(type);
    this.classReader = classReader;
  }

  /**
   * This method should not be used out side of this type!
   * <p>
   * The intention for this method is to reduce the number of exceptions being generated.
   *
   * @param type   the internal type, or class, name to look for.
   * @param loader the class loader to start from
   * @param thro   whether or not to throw the exception on a error.
   * @return either the {@link ClassInfo} for the given type, or null if it could not be found. If
   * {@code thro} is {@code true} then a {@link ClassInfoNotFoundException} is throw when the class
   * cannot be found.
   */
  @SuppressWarnings("Duplicates")
  static ClassInfo loadClassInfo(final String type, final ClassLoader loader, final boolean thro) {
    final String clazzFile = type.replace('.', '/') + ".class";
    final URL resource;
    if (loader == null) {
      // if the class loader is null then try loading the class file from the system class loader
      resource = ClassLoader.getSystemResource(clazzFile);
    } else {
      resource = loader.getResource(clazzFile);
    }

    if (resource == null) {
      // it is theoretically impossible for this to happen
      // if the class loader is null, then we must be a class from system/boot (ie a java class)
      // and therefore it should not be possible for the system class loader to no be able to
      // get the resource, however this is here in case this does happen and to prevent
      // NullPointerExceptions from later in this method.
      if (loader == null) {
        if (thro) {
          throw new ClassInfoNotFoundException("ClassInfo: Class not found on " + type, type);
        }
        return null;
      }

      // both lucee and railo class loaders do not implement ClassLoader#findResource
      // so we have to ask for it via the ClassLoader#getResourceAsStream
      if (isLuceeClassLoader(loader) || isRailoClassLoader(loader)) {
        final InputStream resourceAsStream = loader.getResourceAsStream(clazzFile);
        if (resourceAsStream != null) {
          try {
            return readFromResource(type, resourceAsStream, loader);
          } catch (final IOException e) {
            LOGGER.warn("loadClassInfo - Cannot load class: {}", type);
            LOGGER.debug("loadClassInfo - Cannot load class: {}", type, e);

            if (thro) {
              throw new ClassInfoNotFoundException("ClassInfo: Class not found on " + type, type,
                  e);
            }
          }
        }
      }

      // Some class loaders (lucee) do not propagate the getResource call up to the parent.
      if (isSafeLoader(loader)) {
        final ClassLoader parent = loader.getParent();
        return loadClassInfo(type, parent, thro);
      }

      if (thro) {
        throw new ClassInfoNotFoundException("ClassInfo: Class not found on " + type, type);
      }
      return null;
    }

    try {
      return readFromResource(type, resource, loader);
    } catch (final IOException e) {
      LOGGER.warn("loadClassInfo - Cannot load class: {}", type);
      LOGGER.debug("loadClassInfo - Cannot load class: {}", type, e);

      if (thro) {
        throw new ClassInfoNotFoundException("ClassInfo: Class not found on " + type, type, e);
      }
    }
    return null;
  }

  static boolean isRailoClassLoader(final ClassLoader loader) {
    return loader.getClass().getName().equals(RAILO_LOADER);
  }

  static boolean isLuceeClassLoader(final ClassLoader loader) {
    return loader.getClass().getName().equals(LUCEE_LOADER);
  }

  static boolean isSafeLoader(final ClassLoader loader) {
    final String name = loader.getClass().getName();
    return SAFE_LOADERS.contains(name);
  }

  private static ClassInfo readFromResource(final String type, final URL resource,
      final ClassLoader loader)
      throws IOException {
    return readFromResource(type, resource.openStream(), loader);
  }

  private static ClassInfo readFromResource(final String type,
      final InputStream resource,
      final ClassLoader loader)
      throws IOException {
    try {
      final ClassReader classReader = new ClassReader(resource);
      return new ClassInfo(type, loader, classReader);
    } finally {
      if (resource != null) {
        try {
          resource.close();
        } catch (Exception ignored) {
        }
      }
    }
  }

  Type getType() {
    return type;
  }


  int getModifiers() {
    return this.classReader.getAccess();
  }


  ClassInfo getSuperclass() {
    final String superName = classReader.getSuperName();
    if (superName == null) {
      return null;
    }
    return ClassInfo.loadClassInfo(superName, loader, true);
  }


  ClassInfo[] getInterfaces() {
    final String[] interfaces = classReader.getInterfaces();
    if (interfaces == null) {
      return new ClassInfo[0];
    }
    ClassInfo[] result = new ClassInfo[interfaces.length];
    for (int i = 0; i < result.length; ++i) {
      result[i] = ClassInfo.loadClassInfo(interfaces[i], loader, true);
    }
    return result;
  }


  boolean isInterface() {
    return (getModifiers() & Opcodes.ACC_INTERFACE) > 0;
  }


  public boolean implementsInterface(final ClassInfo that) {
    for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
      ClassInfo[] tis = c.getInterfaces();
      for (ClassInfo ti : tis) {
        if (ti.type.equals(that.type) || ti.implementsInterface(that)) {
          return true;
        }
      }
    }
    return false;
  }


  private boolean isSubclassOf(final ClassInfo that) {
    for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
      if (c.getSuperclass() != null && c.getSuperclass().type.equals(that.type)) {
        return true;
      }
    }
    return false;
  }


  public boolean isAssignableFrom(final ClassInfo that) {
    if (this == that) {
      return true;
    }

    if (that.isSubclassOf(this)) {
      return true;
    }

    if (that.implementsInterface(this)) {
      return true;
    }

    return that.isInterface() && getType().getDescriptor().equals("Ljava/lang/Object;");
  }
}
