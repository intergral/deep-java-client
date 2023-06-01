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

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.reflect.ReflectionImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

public class ReflectionUtils {

  private static final IReflection reflection;

  static {
    if (Utils.getVersion() >= 9) {
      reflection = new com.intergral.deep.reflect.Java9ReflectionImpl();
    } else {
      reflection = new ReflectionImpl();
    }
  }

  public static IReflection getReflection() {
    return reflection;
  }

  public static boolean setAccessible(final Class<?> clazz, final Field field) {
    return getReflection().setAccessible(clazz, field);
  }


  public static boolean setAccessible(final Class<?> clazz, final Method method) {
    return getReflection().setAccessible(clazz, method);
  }


  public static <T> T callMethod(Object target, String methodName, Object... args) {
    return getReflection().callMethod(target, methodName, args);
  }


  public static Method findMethod(Class<?> clazz, String methodName, Class<?>... argTypes) {
    return getReflection().findMethod(clazz, methodName, argTypes);
  }


  public static Field getField(Object target, String fieldName) {
    return getReflection().getField(target, fieldName);
  }

  public static <T> T getFieldValue(Object target, String fieldName) {
    return getReflection().getFieldValue(target, fieldName);
  }

  public static Iterator<Field> getFieldIterator(final Class<?> clazz) {
    return getReflection().getFieldIterator(clazz);
  }

  public static <T> T callField(final Object target, final Field field) {
    return getReflection().callField(target, field);
  }

  public static Set<String> getModifiers(final Field field) {
    return getReflection().getModifiers(field);
  }
}
