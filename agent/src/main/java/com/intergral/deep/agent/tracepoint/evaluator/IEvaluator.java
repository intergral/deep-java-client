package com.intergral.deep.agent.tracepoint.evaluator;

import java.util.Map;

public interface IEvaluator
{
    boolean evaluate( final String expression, final Map<String, Object> values );

    Object evaluateExpression( final String expression, final Map<String, Object> values );
}
