package com.intergral.deep.agent.tracepoint.inst.asm;

public class SkipException extends Error
{

    public SkipException()
    {
        super();
        setStackTrace( new StackTraceElement[0] );
    }
}
