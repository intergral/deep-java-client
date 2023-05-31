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

package com.intergral.deep.agent.types.snapshot;

import java.util.ArrayList;
import java.util.List;

public class Variable
{
    private final String varType;
    private final String valString;
    private final String identityCode;
    private final boolean truncated;
    private final List<VariableID> children = new ArrayList<>();

    public Variable( final String varType, final String valString, final String identityCode, final boolean truncated )
    {

        this.varType = varType;
        this.valString = valString;
        this.identityCode = identityCode;
        this.truncated = truncated;
    }

    public void addChild( final VariableID variableId )
    {
        this.children.add( variableId );
    }

    public String getVarType()
    {
        return varType;
    }

    public String getValString()
    {
        return valString;
    }

    public String getIdentityCode()
    {
        return identityCode;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public List<VariableID> getChildren()
    {
        return children;
    }
}
