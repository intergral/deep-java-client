package com.intergral.deep.agent.tracepoint.cf;

import com.intergral.deep.agent.ReflectionUtils;
import com.intergral.deep.agent.tracepoint.evaluator.IEvaluator;

import java.lang.reflect.Method;
import java.util.Map;

public class CFUtils
{
    private CFUtils()
    {
    }


    public static IEvaluator findCfEval( final Map<String, Object> map )
    {
        final Object that = map.get( "this" );
        if( isLucee( that ) )
        {
            return findLuceeEvaluator( map );
        }
        final Object page = CFUtils.findPage( map );
        if( page == null )
        {
            return null;
        }

        Method evaluate = ReflectionUtils.findMethod( page.getClass(), "Evaluate", String.class );
        if( evaluate == null )
        {
            evaluate = ReflectionUtils.findMethod( page.getClass(), "Evaluate", Object.class );
            if( evaluate == null )
            {
                return null;
            }
        }
        return new CFEvaluator( page, evaluate );
    }


    private static IEvaluator findLuceeEvaluator( final Map<String, Object> map )
    {
        final Object param0 = map.get( "param0" );
        if( param0 == null || !param0.getClass().getName().equals( "lucee.runtime.PageContextImpl" ) )
        {
            return null;
        }

        final Method evaluate = ReflectionUtils.findMethod( param0.getClass(), "evaluate", String.class );
        if( evaluate == null )
        {
            return null;
        }

        return new CFEvaluator( param0, evaluate );
    }


    public static String findUDFName( final Map<String, Object> variables, final String className, final int i )
    {
        if( i == 0 )
        {
            final Object aThis = variables.get( "this" );
            if( aThis == null || !isUDF( aThis ) )
            {
                return null;
            }
            // explicitly set to Object as otherwise the call to String.valueOf can become the char[] version.
            return String.valueOf( ReflectionUtils.<Object>getFieldValue( aThis, "key" ) );
        }
        else if( className.startsWith( "cf" ) && className.contains( "$func" ) )
        {
            return className.substring( className.indexOf( "$func" ) + 5 );
        }
        else
        {
            return null;
        }
    }


    private static boolean isUDF( final Object aThis )
    {
        final Class<?> superclass = aThis.getClass().getSuperclass();
        return superclass != null && superclass.getName().equals( "coldfusion.runtime.UDFMethod" );
    }


    static boolean isScope( final Object varScope )
    {
        return (varScope instanceof Map)
                && (varScope.getClass().getName().startsWith( "coldfusion" ) && varScope.getClass().getName().contains( "Scope" )
                || varScope.getClass().getName().equals( "coldfusion.runtime.ArgumentCollection" ));
    }


    static boolean isLuceeScope( final Object varScope )
    {
        return (varScope instanceof Map)
                && (varScope.getClass().getName().startsWith( "lucee" ) && varScope.getClass().getName().contains( "scope" ));
    }


    public static Object findPage( final Map<String, Object> localVars )
    {
        final Object aThis = localVars.get( "this" );
        if( aThis == null )
        {
            return null;
        }
        if( isUDF( aThis ) )
        {
            return localVars.get( "parentPage" );
        }
        else
        {
            return aThis;
        }
    }


    public static Object findPageContext( final Map<String, Object> localVars )
    {
        final Object page = findPage( localVars );
        if( page == null )
        {
            return null;
        }
        return ReflectionUtils.getFieldValue( page, "pageContext" );
    }


    public static boolean isCfClass( final String classname )
    {
        return classname.startsWith( "cf" ) || classname.endsWith( "$cf" );
    }


    public static boolean isCFFile( final String fileName )
    {
        if( fileName == null )
        {
            return false;
        }
        return fileName.endsWith( ".cf" ) || fileName.endsWith( ".cfc" ) || fileName.endsWith( ".cfm" )
                || fileName.endsWith( ".cfml" );
    }


    public static boolean isLucee( final Object that )
    {
        return that != null
                && that.getClass().getSuperclass() != null
                && (that.getClass().getSuperclass().getName().equals( "lucee.runtime.PageImpl" )
                || that.getClass().getSuperclass().getName().equals( "lucee.runtime.ComponentPageImpl" ));
    }


    public static String guessSource( final String classname )
    {
        if( !classname.endsWith( "$cf" ) )
        {
            return null;
        }
        return classname
                .replaceAll( "\\.", "/" )
                .substring( 0, classname.length() - 3 )
                .replace( "_", "." );
    }
}
