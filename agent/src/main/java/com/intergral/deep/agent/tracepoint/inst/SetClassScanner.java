package com.intergral.deep.agent.tracepoint.inst;


import java.util.Set;

public class SetClassScanner implements IClassScanner
{
    private final Set<String> classNames;


    public SetClassScanner( final Set<String> classNames )
    {
        this.classNames = classNames;
    }


    @Override
    public boolean scanClass( final Class<?> allLoadedClass )
    {
        return this.classNames.contains( InstUtils.internalClass( allLoadedClass ) ) ||
                allLoadedClass.getName().contains( "$" ) &&
                        this.classNames.contains( InstUtils.internalClassStripInner( allLoadedClass ) );
    }
}
