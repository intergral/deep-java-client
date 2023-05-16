package com.intergral.deep.reflect;

import com.intergral.deep.agent.api.reflection.IReflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
}
