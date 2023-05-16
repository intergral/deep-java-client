/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.intergral.deep.agent.api.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author nwightma
 */
public interface IReflection
{
    boolean setAccessible( final Class<?> clazz, final Field field );


    boolean setAccessible( final Class<?> clazz, final Method method );


    <T> T callMethod( Object target, String methodName, Object... args );


    Method findMethod( Class<?> aClass, String methodName, Class<?>... argTypes );


    Field getField( Object target, String fieldName );


    /**
     * Get a field from an object
     *
     * @param target    the object to look at
     * @param fieldName the field name to look for
     * @param <T>       the type to return as
     *
     * @return the field as T, else {@code null}
     */
    <T> T getFieldValue( Object target, String fieldName );
}
