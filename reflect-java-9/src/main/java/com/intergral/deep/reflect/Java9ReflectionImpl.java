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
