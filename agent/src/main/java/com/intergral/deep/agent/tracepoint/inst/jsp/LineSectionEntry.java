/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.jsp;

public class LineSectionEntry
{
    private final int inputStartLine;
    private final int lineFileID;
    private final int repeatCount;
    private final int outputStartLine;
    private final int outputLineIncrement;


    public LineSectionEntry( final int inputStartLine, final int lineFileID, final int repeatCount, final int outputStartLine, final int outputLineIncrement )
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
