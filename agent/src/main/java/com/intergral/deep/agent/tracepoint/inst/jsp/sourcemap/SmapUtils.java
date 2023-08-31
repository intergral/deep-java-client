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

package com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap;

import static org.objectweb.asm.Opcodes.ASM7;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmapUtils {

  private final static Logger LOGGER = LoggerFactory.getLogger(SmapUtils.class);
  private SmapUtils() {
  }


  public static String lookUp(final Class c) {
    final String clazzFile = c.getName().replace('.', '/') + ".class";

    ClassLoader loader = c.getClassLoader();
    while (loader != null) {
      final URL resource = loader.getResource(clazzFile);
      if (resource != null) {
        try {
          final InputStream in = resource.openStream();
          return parseStream(in);
        } catch (Throwable t) {
          LOGGER.error("Could not parse source map for class {}", c.getName(), t);
        }
      } else {
        try {
          final InputStream in = loader.getResourceAsStream(clazzFile);
          if (in != null) {
            return parseStream(in);
          }
        } catch (Throwable t) {
          LOGGER.error("Could not parse source map for class {}", c.getName(), t);
        }
      }

      // move up
      loader = loader.getParent();
    }

    final URL resource = ClassLoader.getSystemResource(clazzFile);
    if (resource != null) {
      try {
        final InputStream in = resource.openStream();
        return parseStream(in);
      } catch (Throwable t) {
        LOGGER.error("Could not parse source map for class {}", c.getName(), t);
      }
    }

    return null;
  }


  public static String parseBytes(final byte[] bytes) throws IOException {
    final ClassReader reader = new ClassReader(bytes);
    return scan(reader);
  }


  public static String parseStream(final InputStream in) throws IOException {
    final ClassReader reader = new ClassReader(in);
    return scan(reader);
  }


  private static String scan(ClassReader reader) {
    final Visitor v = new Visitor();
    reader.accept(v, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    return v.getDebug();
  }


  public static String scanSource(ClassReader reader) {
    final Visitor v = new Visitor();
    reader.accept(v, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    return v.getSource();
  }

  public static class Visitor extends ClassVisitor {

    private String source;
    private String debug;


    public Visitor() {
      super(ASM7);
    }


    @Override
    public void visitSource(String source, String debug) {
      super.visitSource(source, debug);

      this.source = source;
      this.debug = debug;
    }


    public String getSource() {
      return source;
    }


    public String getDebug() {
      return debug;
    }

  }
}
