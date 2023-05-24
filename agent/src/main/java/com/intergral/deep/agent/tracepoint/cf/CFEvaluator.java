package com.intergral.deep.agent.tracepoint.cf;

import com.intergral.deep.agent.tracepoint.evaluator.AbstractEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class CFEvaluator extends AbstractEvaluator
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CFEvaluator.class );

    private final Object page;
    private final Method evaluate;


    public CFEvaluator( final Object page, final Method evaluate )
    {
        this.page = page;
        this.evaluate = evaluate;
    }


    @Override
    public Object evaluateExpression( final String expression, final Map<String, Object> values )
    {
        try
        {
            return evaluate.invoke( page, expression );
        }
        catch( IllegalAccessException | InvocationTargetException e )
        {
            LOGGER.debug( "Unable to evaluate expression {}", expression );
        }
        return null;
    }
}
