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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

public class BasicAuthProvider implements IAuthProvider
{
    private final static Logger LOGGER = LoggerFactory.getLogger( BasicAuthProvider.class );
    private final ISettings settings;

    public BasicAuthProvider( final ISettings settings )
    {
        this.settings = settings;
    }

    @Override
    public Map<String, String> provide()
    {
        final String userName = settings.getSettingAs( "service.username", String.class );
        final String password = settings.getSettingAs( "service.password", String.class );
        if( userName == null || password == null )
        {
            return Collections.emptyMap();
        }
        final String encodedString = Base64.getEncoder()
                .encodeToString( String.format( "%s:%s", userName, password ).getBytes() );
        return Collections.singletonMap( "authorization", encodedString );
    }
}
