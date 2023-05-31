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
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class LineSectionEntry
{
    private final int inputStartLine;
    private final int lineFileID;
    private final int repeatCount;
    private final int outputStartLine;
    private final int outputLineIncrement;


    public LineSectionEntry( final int inputStartLine, final int lineFileID, final int repeatCount,
                             final int outputStartLine, final int outputLineIncrement )
    {
        this.inputStartLine = inputStartLine;
        this.lineFileID = lineFileID;
        this.repeatCount = repeatCount;
        this.outputStartLine = outputStartLine;
        this.outputLineIncrement = outputLineIncrement;
    }


    public int getInputStartLine()
    {
        return inputStartLine;
    }


    public int getLineFileID()
    {
        return lineFileID;
    }


    public int getRepeatCount()
    {
        return repeatCount;
    }


    public int getOutputStartLine()
    {
        return outputStartLine;
    }


    public int getOutputLineIncrement()
    {
        return outputLineIncrement;
    }


    @Override
    public String toString()
    {
        return "LineSectionEntry#" + lineFileID + " " + inputStartLine + " " + repeatCount + " " + outputStartLine + " "
                + outputLineIncrement;
    }
}
