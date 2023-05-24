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

package com.intergral.deep;

import java.util.HashMap;
import java.util.Map;

public class DeepConfigBuilder
{
    private final Map<String, Object> config = new HashMap<>();

    public void start()
    {
        Deep.getInstance().startWithConfig( this.build() );
    }

    public DeepConfigBuilder setValue( final String key, final Object value )
    {
        this.config.put( key, value );
        return this;
    }

    private String build()
    {
        return this.configAsArgs( this.config );
    }

    /**
     * Convert the config to a string
     *
     * @param config the config to parse
     * @return the config as a string
     */
    String configAsArgs( final Map<String, Object> config )
    {
        final StringBuilder stringBuilder = new StringBuilder();
        for( Map.Entry<String, Object> entry : config.entrySet() )
        {
            if( entry.getValue() == null )
            {
                continue;
            }
            if( stringBuilder.length() != 0 )
            {
                stringBuilder.append( ',' );
            }
            if( entry.getValue() instanceof Map )
            {
                //noinspection unchecked
                stringBuilder.append( entry.getKey() )
                        .append( '=' )
                        .append( configAsArgs( (Map<String, Object>) entry.getValue() ) );
            }
            else
            {
                stringBuilder.append( entry.getKey() ).append( '=' ).append( entry.getValue() );
            }
        }
        return stringBuilder.toString();
    }

}
