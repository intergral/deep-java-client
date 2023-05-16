package com.intergral.deep.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Java9ReflectionImpl extends ReflectionImpl
{

    @Override
    public boolean setAccessible( final Class<?> clazz, final Field field )
    {
        try
        {
            openModule( clazz );

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
            openModule( clazz );

            method.setAccessible( true );
            return true;
        }
        catch( final Exception e )
        {
            return false;
        }
    }


    private void openModule( final Class<?> clazz )
    {
        final Module m = clazz.getModule();
        if( m.isNamed() )
        {
            final String pkgName = clazz.getPackageName();
            m.addOpens( pkgName, getClass().getModule() );
        }
    }
}
