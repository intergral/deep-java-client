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

import com.intergral.deep.agent.api.DeepRuntimeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * This type exposes helpful reflection methods.
 */
public interface IReflection {

  /**
   * Set the field as accessible.
   *
   * @param clazz the clazz the field is on
   * @param field the field to access
   * @return could we complete the operation
   * @see Field#setAccessible(boolean)
   */
  boolean setAccessible(final Class<?> clazz, final Field field);

  /**
   * Set the method as accessible.
   *
   * @param clazz  the clazz the field is on
   * @param method the method to access
   * @return could we complete the operation
   * @see Method#setAccessible(boolean)
   */
  boolean setAccessible(final Class<?> clazz, final Method method);

  /**
   * Set the constructor as accessible.
   *
   * @param clazz       the clazz the field is on
   * @param constructor the constructor to set
   * @return could we complete the operation
   * @see Constructor#setAccessible(boolean)
   */
  boolean setAccessible(final Class<?> clazz, final Constructor<?> constructor);

  /**
   * Call a method on the target object.
   * <p>
   * This will look for the method using {@link #findMethod(Class, String, Class[])} using the input arguments as the argument types of the
   * method. The method will then be invoked on the target argument.
   *
   * @param target     the target instance to call the method on
   * @param methodName the name of the method to call
   * @param args       the arguments to the method
   * @param <T>        the type of the response
   * @return the response of the method
   */
  <T> T callMethod(Object target, String methodName, Object... args);

  /**
   * Scan the hierarchy of the input class for a method with the given name. This will scan declared methods and methods, as well as
   * stepping up the super class.
   *
   * @param clazz      the class to start the scan on
   * @param methodName the name of the method to look for
   * @param argTypes   the argument types in the method signature
   * @return the discovered method or {@code null}
   */
  Method findMethod(Class<?> clazz, String methodName, Class<?>... argTypes);

  /**
   * Get a filed from the target.
   *
   * @param target    the target instance to look on.
   * @param fieldName the name of the field to get
   * @return the field, or {@code null}
   */
  Field getField(Object target, String fieldName);


  /**
   * Get a field from an object.
   *
   * @param target    the object to look at
   * @param fieldName the field name to look for
   * @param <T>       the type to return as
   * @return the field as T, else {@code null}
   */
  <T> T getFieldValue(Object target, String fieldName);

  /**
   * Get an iterator that will iterator over all the available fields on the given class.
   *
   * @param clazz the class to scan
   * @return the iterator for the fields
   */
  Iterator<Field> getFieldIterator(Class<?> clazz);

  /**
   * Call a field on a target.
   *
   * @param target the object to get the field from
   * @param field  the field to call
   * @param <T>    the type of the return
   * @return the value of the field
   */
  <T> T callField(Object target, Field field);

  /**
   * Get the modifier names of a field.
   *
   * @param field the field to look at
   * @return a set of strings that represent the modifiers.
   */
  Set<String> getModifiers(Field field);

  /**
   * Find a constructor on the given class.
   *
   * @param clazz the class to look on
   * @param args  the arguments for the constructor
   * @return the constructor or {@code null}
   */
  Constructor<?> findConstructor(final Class<?> clazz, final Class<?>... args);

  /**
   * Call a constructor with the arguments.
   *
   * @param constructor the constructor to call
   * @param args        the arguments for the constructor
   * @param <T>         the type of the return
   * @return the new object, or {@code null}
   * @throws DeepRuntimeException if we could not create a new object
   */
  <T> T callConstructor(final Constructor<?> constructor, final Object... args) throws DeepRuntimeException;
}
