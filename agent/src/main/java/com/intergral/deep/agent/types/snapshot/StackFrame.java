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

import java.util.Collection;

public class StackFrame
{
    private final String className;
    private final String fileName;
    private final String methodName;
    private final int lineNumber;
    private final boolean appFrame;
    private final boolean nativeFrame;
    private final Collection<VariableID> frameVariables;

    public StackFrame( final String fileName, final int lineNumber, final String className,
                       final String methodName, final boolean appFrame, final boolean nativeFrame,
                       final Collection<VariableID> frameVariables )
    {

        this.className = className;
        this.fileName = fileName;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.appFrame = appFrame;
        this.nativeFrame = nativeFrame;
        this.frameVariables = frameVariables;
    }

    public String getClassName()
    {
        return className;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public boolean isAppFrame()
    {
        return appFrame;
    }

    public boolean isNativeFrame()
    {
        return nativeFrame;
    }

    public Collection<VariableID> getFrameVariables()
    {
        return frameVariables;
    }
}
