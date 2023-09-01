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

package com.intergral.deep.tests.inst;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ByteClassLoader extends ClassLoader {

  private final Map<String, byte[]> bytes = new HashMap<>();

  public static byte[] loadBytes(final String name) throws IOException {
    final byte[] bytes;
    try (InputStream resourceAsStream = ByteClassLoader.class.getResourceAsStream("/" + name + ".class")) {
      bytes = new byte[resourceAsStream.available()];
      resourceAsStream.read(bytes);
    }
    return bytes;
  }

  public static ByteClassLoader forFile(final String name) throws IOException {
    final byte[] loadedBytes = loadBytes(name);
    final ByteClassLoader byteClassLoader = new ByteClassLoader();
    byteClassLoader.setBytes(name, loadedBytes);
    return byteClassLoader;
  }

  public void setBytes(final String name, final byte[] bytes) {
    this.bytes.put(name, bytes);
  }

  public byte[] getBytes(final String name) {
    return this.bytes.get(name);
  }

  @Override
  public Class<?> loadClass(final String name) throws ClassNotFoundException {
    final byte[] bytes = this.bytes.get(name);
    if (bytes != null) {
      return defineClass(name, bytes, 0, bytes.length);
    }
    return super.loadClass(name);
  }

  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    final byte[] bytes = this.bytes.get(name);
    if (bytes != null) {
      return defineClass(name, bytes, 0, bytes.length);
    }
    return super.findClass(name);
  }
}
