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
