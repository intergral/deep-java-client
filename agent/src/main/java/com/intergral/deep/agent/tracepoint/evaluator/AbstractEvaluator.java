package com.intergral.deep.agent.tracepoint.evaluator;

import java.util.Map;

public abstract class AbstractEvaluator implements IEvaluator
{
    @Override
    public boolean evaluate( final String expression, final Map<String, Object> values )
    {
        return objectToBoolean( evaluateExpression( expression, values ) );
    }


    public static boolean objectToBoolean( final Object obj )
    {
        if( obj == null )
        {
            return false;
        }

        if( obj instanceof Boolean )
        {
            return (Boolean) obj;
        }

        if( obj instanceof Number )
        {
            return ((Number) obj).intValue() != 0;
        }

        if( obj instanceof String )
        {
            return Boolean.parseBoolean( String.valueOf( obj ) );
        }

        return true;
    }
}
