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

package com.intergral.deep.plugins.cf;

import com.intergral.deep.agent.api.plugin.IEventContext;
import com.intergral.deep.agent.api.plugin.IPlugin;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;

import java.util.HashMap;

public class CFPlugin implements IPlugin
{
    @Override
    public Resource decorate( final ISettings settings, final IEventContext context )
    {
        final String appName = context.evaluateExpression( "APPLICATION.applicationname" );

        return Resource.create( new HashMap<String, Object>()
        {{
            put( "cf_version", Utils.loadCFVersion() );
            put( "app_name", appName );
        }} );
    }

    @Override
    public boolean isActive( final ISettings settings )
    {
        if( !Utils.isCFServer() )
        {
            return false;
        }
        return IPlugin.super.isActive( settings );
    }
}
