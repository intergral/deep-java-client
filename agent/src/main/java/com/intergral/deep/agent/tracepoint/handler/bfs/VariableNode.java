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

package com.intergral.deep.agent.tracepoint.handler.bfs;

public class VariableNode
{
    private final String key;
    private final Object value;
    private final Node.IParent frameParent;

    public VariableNode( final String key, final Object value, final Node.IParent frameParent )
    {
        this.key = key;
        this.value = value;
        this.frameParent = frameParent;
    }
}
