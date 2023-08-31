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

package com.intergral.deep.reflect;

import com.intergral.deep.agent.api.DeepRuntimeException;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.utils.ArrayIterator;
import com.intergral.deep.agent.api.utils.CompoundIterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionImpl implements IReflection {

  @Override
  public boolean setAccessible(final Class<?> clazz, final Field field) {
    try {
      field.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  @Override
  public boolean setAccessible(final Class<?> clazz, final Method method) {
    try {
      method.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  @Override
  public boolean setAccessible(final Class<?> clazz, final Constructor<?> constructor) {
    try {
      constructor.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  @Override
  public <T> T callMethod(final Object target, final String methodName, final Object... args) {
    final Class<?>[] argTypes = new Class<?>[args.length];
    for (int i = 0; i < args.length; i++) {
      final Object arg = args[i];
      argTypes[i] = arg.getClass();
    }

    final Method method = findMethod(target.getClass(), methodName, argTypes);
    if (method != null) {
      try {
        setAccessible(target.getClass(), method);
        //noinspection unchecked
        return (T) method.invoke(target, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        return null;
      }
    }
    return null;
  }


  @Override
  public Method findMethod(final Class<?> originalClazz, final String methodName,
      final Class<?>... argTypes) {
    Class<?> clazz = originalClazz;
    while (clazz != null) {
      try {
        return clazz.getMethod(methodName, argTypes);
      } catch (Exception ignored) {
        // ignored
      }
      try {
        return clazz.getDeclaredMethod(methodName, argTypes);
      } catch (Exception e) {
        clazz = clazz.getSuperclass();
      }
    }
    return null;
  }


  public Field getField(final Object obj, final String fieldName) {
    Class<?> clazz = obj.getClass();
    while (clazz != null) {
      try {
        return clazz.getField(fieldName);
      } catch (Exception e) {
        try {

          return clazz.getDeclaredField(fieldName);
        } catch (Exception e1) {
          clazz = clazz.getSuperclass();
        }
      }
    }
    return null;
  }


  @Override
  public <T> T getFieldValue(final Object target, final String fieldName) {
    try {
      final Field field = getField(target, fieldName);

      return callField(target, field);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Iterator<Field> getFieldIterator(final Class<?> clazz) {
    if (clazz == null || clazz == Object.class) {
      return new Iterator<Field>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public Field next() {
          return null;
        }
      };
    }
    final Field[] fields = clazz.getFields();
    final Field[] declaredFields = clazz.getDeclaredFields();
    return new CompoundIterator<>(new ArrayIterator<>(fields), new ArrayIterator<>(declaredFields));
  }

  @Override
  public <T> T callField(final Object target, final Field field) {
    try {
      if (field == null) {
        return null;
      }
      setAccessible(field.getDeclaringClass(), field);
      //noinspection unchecked
      return (T) field.get(target);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Set<String> getModifiers(final Field field) {
    final int modifiers = field.getModifiers();
    if (modifiers == 0) {
      return Collections.emptySet();
    }
    final String string = Modifier.toString(modifiers);
    return Arrays.stream(string.split(" ")).collect(Collectors.toSet());
  }

  @Override
  public Constructor<?> findConstructor(final Class<?> clazz, final Class<?>... args) {
    try {
      return clazz.getConstructor(args);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @Override
  public <T> T callConstructor(final Constructor<?> constructor, final Object... args) {
    try {
      setAccessible(constructor.getDeclaringClass(), constructor);
      //noinspection unchecked
      return (T) constructor.newInstance(args);
    } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
      throw new DeepRuntimeException("Cannot call constructor: " + constructor, e);
    }
  }
}
