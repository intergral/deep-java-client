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

package com.intergral.deep.agent;

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.reflect.ReflectionImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

public class ReflectionUtils
{
    private final static IReflection reflection;

    static
    {
        if( Utils.getVersion() >= 9 )
        {
            reflection = new com.intergral.deep.reflect.Java9ReflectionImpl();
        }
        else
        {
            reflection = new ReflectionImpl();
        }
    }

    public static IReflection getReflection()
    {
        return reflection;
    }

    public static boolean setAccessible( final Class<?> clazz, final Field field )
    {
        return getReflection().setAccessible( clazz, field );
    }


    public static boolean setAccessible( final Class<?> clazz, final Method method )
    {
        return getReflection().setAccessible( clazz, method );
    }


    public static <T> T callMethod( Object target, String methodName, Object... args )
    {
        return getReflection().callMethod( target, methodName, args );
    }


    public static Method findMethod( Class<?> aClass, String methodName, Class<?>... argTypes )
    {
        return getReflection().findMethod( aClass, methodName, argTypes );
    }


    public static Field getField( Object target, String fieldName )
    {
        return getReflection().getField( target, fieldName );
    }

    public static <T> T getFieldValue( Object target, String fieldName )
    {
        return getReflection().getFieldValue( target, fieldName );
    }

    public static Iterator<Field> getFieldIterator( final Class<?> clazz )
    {
        return getReflection().getFieldIterator(clazz);
    }

    public static <T> T callField( final Object target, final Field field )
    {
        return getReflection().callField(target, field);
    }

    public static Set<String> getModifiers( final Field field )
    {
        return getReflection().getModifiers(field);
    }
}
