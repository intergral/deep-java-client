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

package com.intergral.deep.agent.api.auth;

import com.intergral.deep.agent.api.settings.ISettings;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public class AuthProvider
{
    private final static NoopProvider NOOP_PROVIDER = new NoopProvider();

    public static IAuthProvider provider( final ISettings settings )
    {
        final String serviceAuthProvider = settings.getSettingAs( "service.auth_provider", String.class );
        if( serviceAuthProvider == null || serviceAuthProvider.trim().isEmpty() )
        {
            return NOOP_PROVIDER;
        }
        try
        {
            final Class<?> aClass = Class.forName( serviceAuthProvider );
            final Constructor<?> constructor = aClass.getConstructor( ISettings.class );
            final Object newInstance = constructor.newInstance( settings );
            return (IAuthProvider) newInstance;
        }
        catch( ClassNotFoundException |
               NoSuchMethodException |
               InvocationTargetException |
               InstantiationException |
               IllegalAccessException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static class NoopProvider implements IAuthProvider
    {

        @Override
        public Map<String, String> provide()
        {
            return Collections.emptyMap();
        }
    }
}
