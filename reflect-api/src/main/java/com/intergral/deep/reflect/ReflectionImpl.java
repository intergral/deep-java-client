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

package com.intergral.deep.reflect;

import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.api.utils.ArrayIterator;
import com.intergral.deep.agent.api.utils.CompoundIterator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionImpl implements IReflection
{
    @Override
    public boolean setAccessible( final Class<?> clazz, final Field field )
    {
        try
        {
            field.setAccessible( true );
            return true;
        }
        catch( final Exception e )
        {
            return false;
        }
    }


    @Override
    public boolean setAccessible( final Class<?> clazz, final Method method )
    {
        try
        {
            method.setAccessible( true );
            return true;
        }
        catch( final Exception e )
        {
            return false;
        }

    }


    @Override
    public <T> T callMethod( final Object target, final String methodName, final Object... args )
    {
        final Class<?>[] argTypes = new Class<?>[args.length];
        for( int i = 0; i < args.length; i++ )
        {
            final Object arg = args[i];
            argTypes[i] = arg.getClass();
        }

        final Method method = findMethod( target.getClass(), methodName, argTypes );
        if( method != null )
        {
            try
            {
                setAccessible( target.getClass(), method );
                return (T) method.invoke( target, args );
            }
            catch( IllegalAccessException | InvocationTargetException e )
            {
                return null;
            }
        }
        return null;
    }


    @Override
    public Method findMethod( final Class<?> aClass, final String methodName, final Class<?>... argTypes )
    {
        Class<?> clazz = aClass;
        while( clazz != null )
        {
            try
            {
                return clazz.getMethod( methodName, argTypes );
            }
            catch( Exception e )
            {
                // ignored
            }
            try
            {
                return clazz.getDeclaredMethod( methodName, argTypes );
            }
            catch( Exception e )
            {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }


    public Field getField( final Object obj, final String fieldName )
    {
        Class<?> aClass = obj.getClass();
        while( aClass != null )
        {
            try
            {
                return aClass.getField( fieldName );
            }
            catch( Exception e )
            {
                try
                {

                    return aClass.getDeclaredField( fieldName );
                }
                catch( Exception e1 )
                {
                    aClass = aClass.getSuperclass();
                }
            }
        }
        return null;
    }


    @Override
    public <T> T getFieldValue( final Object target, final String fieldName )
    {
        try
        {
            final Field field = getField( target, fieldName );

            return callField( target, field );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public Iterator<Field> getFieldIterator( final Class<?> clazz )
    {
        if( clazz == null || clazz == Object.class )
        {
            return new Iterator<Field>()
            {
                @Override
                public boolean hasNext()
                {
                    return false;
                }

                @Override
                public Field next()
                {
                    return null;
                }
            };
        }
        final Field[] fields = clazz.getFields();
        final Field[] declaredFields = clazz.getDeclaredFields();
        //noinspection unchecked
        return new CompoundIterator<>( new ArrayIterator<>( fields ), new ArrayIterator<>( declaredFields ) );
    }

    @Override
    public <T> T callField( final Object target, final Field field )
    {
        try
        {
            if( field == null )
            {
                return null;
            }
            setAccessible( field.getDeclaringClass(), field );
            //noinspection unchecked
            return (T) field.get( target );
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public Set<String> getModifiers( final Field field )
    {
        final int modifiers = field.getModifiers();
        final String string = Modifier.toString( modifiers );
        return Arrays.stream( string.split( " " ) ).collect( Collectors.toSet() );
    }
}
