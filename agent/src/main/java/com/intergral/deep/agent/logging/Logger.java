/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
