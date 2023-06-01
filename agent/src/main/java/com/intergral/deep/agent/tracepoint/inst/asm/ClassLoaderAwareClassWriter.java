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


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassLoaderAwareClassWriter extends ClassWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassLoaderAwareClassWriter.class);

  private static final String JAVA_LANG_OBJECT = "java/lang/Object";

  private final ClassLoader classLoader;


  public ClassLoaderAwareClassWriter(final ClassReader classReader, final int flags,
      final ClassLoader loader) {
    super(classReader, flags);
    this.classLoader = loader;
  }


  @Override
  protected String getCommonSuperClass(final String type1, final String type2) {
    if (type1.equals(JAVA_LANG_OBJECT) || type2.equals(JAVA_LANG_OBJECT)) {
      LOGGER.trace("getCommonSuperClass - {} on {} vs {}", JAVA_LANG_OBJECT, type1, type2);
      return JAVA_LANG_OBJECT;
    }
    ClassInfo c;
    ClassInfo d;
    try {
      c = ClassInfo.loadClassInfo(type1, classLoader, true);
      d = ClassInfo.loadClassInfo(type2, classLoader, true);
    } catch (ClassInfoNotFoundException re) {
      // FR-5926 - Oracle ojdbc7 driver looks for this class even through it is not
      // needed for most cases
      if (re.getType().equals("oracle/security/pki/OraclePKIProvider")) {
        return JAVA_LANG_OBJECT;
      }
      // Moved from ClassInfo, so it is not printed when it is OraclePKIProvider
      LOGGER.trace("getCommonSuperClass {}, falling back to ASM default", re.getMessage(), re);

      // Last ditch attempt if we cant file class bytes
      return super.getCommonSuperClass(type1, type2);
    } catch (Throwable e) {
      LOGGER.trace("getCommonSuperClass - Throwable: {} on {} vs {}", e.getMessage(), type1, type2,
          e);
      throw new RuntimeException(e);
    }
    if (c.isAssignableFrom(d)) {
      LOGGER.trace("getCommonSuperClass - {} on {} vs {}", type1, type1, type2);
      return type1;
    }
    if (d.isAssignableFrom(c)) {
      LOGGER.trace("getCommonSuperClass - {} on {} vs {}", type2, type1, type2);
      return type2;
    }
    if (c.isInterface() || d.isInterface()) {
      LOGGER.trace("getCommonSuperClass - {} on {} vs {}", JAVA_LANG_OBJECT, type1, type2);
      return JAVA_LANG_OBJECT;
    } else {
      do {
        c = c.getSuperclass();
      } while (!c.isAssignableFrom(d));
      LOGGER.trace("getCommonSuperClass - {} on {} vs {}", c.getType().getInternalName(), type1,
          type2);
      return c.getType().getInternalName();
    }
  }
}
