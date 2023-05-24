/**
 * Copyright (C) 2019 Intergral Information Solutions GmbH. All Rights Reserved
 */
package com.intergral.deep.agent.tracepoint.inst.asm;

public class ClassInfoNotFoundException extends RuntimeException
{
    private final String type;


    public ClassInfoNotFoundException( final String message, final String type, final Throwable cause )
    {
        super( message, cause );
        this.type = type;
    }


    public ClassInfoNotFoundException( final String message, final String type )
    {
        super( message );
        this.type = type;
    }


    public String getType()
    {
        return type;
    }
}
