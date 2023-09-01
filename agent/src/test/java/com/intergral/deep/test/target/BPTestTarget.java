package com.intergral.deep.test.target;

import com.intergral.deep.agent.tracepoint.handler.Callback;
import java.util.Random;

/**
 * This is a target for many test breakpoints - be careful with line numbers, and do not format the code.
 * <p>
 * The methods ending in _visited are what the code will be converted to (only includes the line ends) and is useful when using the asm
 * plugin to see the code.
 */
// @formatter:off
@SuppressWarnings("ALL") public class BPTestTarget extends BPSuperClass
{
    private String name;


    public BPTestTarget( final String name, int i )
    {
        super("i am a name" + name );
        i += 1;
        this.name = name;
    }


    public String getName_visited()
    {
        if( name == null )
        {
            try{return null;}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
        }
        return name;
    }

    public void setName_visited( final String name )
    {
        try{this.name = name;}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
    }

    public String getName()
    {
        if( name == null )
        {
            return null;
        }
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }


    public String callSomeThing_visited( final String val )
    {
        try{return getName() + val;}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
    }


    public String callSomeThing( final String val )
    {
        return getName() + val;
    }


    public int errorSomething_visited( final String withargs )
    {
        try{return withargs.length();}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
    }


    public int errorSomething( final String withargs )
    {
        return withargs.length();
    }


    public int throwSomething_visited( final String withargs )
    {
        try{throw new RuntimeException(withargs);}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
    }


    public int throwSomething( final String withargs )
    {
        throw new RuntimeException( withargs );
    }


    public String catchSomething_visited( final String withargs )
    {
        try
        {
            throw new RuntimeException( withargs );
        }
        catch( RuntimeException re )
        {
            try{return re.getMessage();}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
        }
    }


    public String catchSomething( final String withargs )
    {
        try
        {
            throw new RuntimeException( withargs );
        }
        catch( RuntimeException re )
        {
            return re.getMessage();
        }
    }


    public String finallySomething_visited( final String withargs )
    {
        try
        {
            throw new RuntimeException( withargs );
        }
        catch( RuntimeException re )
        {
            return re.getMessage();
        }
        finally
        {
            try{System.out.println("something in finally");}catch( Throwable t ){ Callback.callBackException( t ); throw t; }finally {Callback.callBackFinally(null,null);}
        }
    }


    public String finallySomething( final String withargs )
    {
        try
        {
            throw new RuntimeException( withargs );
        }
        catch( RuntimeException re )
        {
            return re.getMessage();
        }
        finally
        {
            System.out.println( "something in finally" );
        }
    }

    public String someFunctionWithABody(final String someArg){
        final String name = this.name;
        final String newName = name + someArg;
        final Random random = new Random();
        final int i = random.nextInt();
        return i + newName;
    }


    public void conditionalThrow( final int val, final int max ) throws Exception
    {
        if( val > max )
        {
            throw new Exception( "Hit max executions " + val + " " + max );
        }
    }


    public void breakSomething()
    {
        for( int i = 0; i < 10; i++ )
        {
            if( i == 5 )
            {
                System.out.println( "do something" );
                break;
            }
        }
    }


    public void continueSomething()
    {
        for( int i = 0; i < 10; i++ )
        {
            if( i == 5 )
            {
                System.out.println( "do something" );
                continue;
            }
            System.out.println( "something else" );
        }
    }
}
//@formatter:on
