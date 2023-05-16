/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
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
