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

package com.intergral.deep.agent.tracepoint.evaluator;

import com.intergral.deep.agent.api.plugin.IEvaluator;

import java.util.Map;

public abstract class AbstractEvaluator implements IEvaluator
{
    @Override
    public boolean evaluate( final String expression, final Map<String, Object> values )
    {
        return objectToBoolean( evaluateExpression( expression, values ) );
    }


    public static boolean objectToBoolean( final Object obj )
    {
        if( obj == null )
        {
            return false;
        }

        if( obj instanceof Boolean )
        {
            return (Boolean) obj;
        }

        if( obj instanceof Number )
        {
            return ((Number) obj).intValue() != 0;
        }

        if( obj instanceof String )
        {
            return Boolean.parseBoolean( String.valueOf( obj ) );
        }

        return true;
    }
}
