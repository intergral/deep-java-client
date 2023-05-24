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

package com.intergral.deep.agent.logging;

import com.intergral.deep.agent.AgentImpl;
import com.intergral.deep.agent.settings.Settings;
import org.slf4j.LoggerFactory;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger
{
    public static org.slf4j.Logger configureLogging( final Settings settings )
    {
        final java.util.logging.Logger logger = java.util.logging.Logger.getLogger( "com.intergral" );
        logger.setUseParentHandlers( false );
        final ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler( handler );

        final Level settingAs = settings.getSettingAs( "logging.level", Level.class );
        handler.setLevel( settingAs );
        logger.setLevel( settingAs );
        return LoggerFactory.getLogger( AgentImpl.class );
    }
}
