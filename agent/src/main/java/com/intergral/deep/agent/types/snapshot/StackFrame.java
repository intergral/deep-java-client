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

import java.util.Collection;

public class StackFrame {

  private final String className;
  private final String fileName;
  private final String methodName;
  private final int lineNumber;
  private final boolean appFrame;
  private final boolean nativeFrame;
  private final Collection<VariableID> frameVariables;
  private final String transpiledFile;
  private final int transpiledLine;

  public StackFrame(final String fileName, final int lineNumber, final String className,
      final String methodName, final boolean appFrame, final boolean nativeFrame,
      final Collection<VariableID> frameVariables, final String transpiledFile, final int transpiledLine) {

    this.className = className;
    this.fileName = fileName;
    this.methodName = methodName;
    this.lineNumber = lineNumber;
    this.appFrame = appFrame;
    this.nativeFrame = nativeFrame;
    this.frameVariables = frameVariables;
    this.transpiledFile = transpiledFile;
    this.transpiledLine = transpiledLine;
  }

  public String getClassName() {
    return className;
  }

  public String getFileName() {
    return fileName;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public boolean isAppFrame() {
    return appFrame;
  }

  public boolean isNativeFrame() {
    return nativeFrame;
  }

  public Collection<VariableID> getFrameVariables() {
    return frameVariables;
  }

  public String getTranspiledFile() {
    return transpiledFile;
  }

  public int getTranspiledLine() {
    return transpiledLine;
  }
}
