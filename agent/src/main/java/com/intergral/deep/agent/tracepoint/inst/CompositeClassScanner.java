package com.intergral.deep.agent.tracepoint.inst;


import com.intergral.deep.agent.tracepoint.inst.asm.TransformerUtils;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompositeClassScanner implements IClassScanner
{
    private final Set<IClassScanner> scanner = new HashSet<>();


    public void addScanner( final IClassScanner classScanner )
    {
        this.scanner.add( classScanner );
    }


    @Override
    public boolean scanClass( final Class<?> clazz )
    {
        for( IClassScanner iClassScanner : scanner )
        {
            if( iClassScanner.scanClass( clazz ) )
            {
                return true;
            }
        }
        return false;
    }


    public Class<?>[] scanAll( final Instrumentation inst )
    {
        final List<Class<?>> classes = new ArrayList<>();
        final Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        for( Class<?> allLoadedClass : allLoadedClasses )
        {
            if( !TransformerUtils.isExcludedClass( allLoadedClass ) &&
                    inst.isModifiableClass( allLoadedClass ) &&
                    scanClass( allLoadedClass ) )
            {
                classes.add( allLoadedClass );
            }
        }
        return classes.toArray( new Class<?>[0] );
    }
}
