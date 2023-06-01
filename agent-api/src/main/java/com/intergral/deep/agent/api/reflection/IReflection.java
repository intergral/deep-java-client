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

package com.intergral.deep.agent.api.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * @author nwightma
 */
public interface IReflection {

  boolean setAccessible(final Class<?> clazz, final Field field);


  boolean setAccessible(final Class<?> clazz, final Method method);


  <T> T callMethod(Object target, String methodName, Object... args);


  Method findMethod(Class<?> clazz, String methodName, Class<?>... argTypes);


  Field getField(Object target, String fieldName);


  /**
   * Get a field from an object
   *
   * @param target    the object to look at
   * @param fieldName the field name to look for
   * @param <T>       the type to return as
   * @return the field as T, else {@code null}
   */
  <T> T getFieldValue(Object target, String fieldName);

  Iterator<Field> getFieldIterator(Class<?> clazz);

  <T> T callField(Object target, Field field);

  Set<String> getModifiers(Field field);
}
