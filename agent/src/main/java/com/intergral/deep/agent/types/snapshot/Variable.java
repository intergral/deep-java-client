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
