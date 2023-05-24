//package com.intergral.deep.agent.tracepoint.cf;
//
//import com.intergral.deep.agent.ReflectionUtils;
//import com.intergral.deep.agent.Utils;
//import com.intergral.deep.agent.tracepoint.evaluator.IEvaluator;
//import com.intergral.deep.agent.tracepoint.handler.SnapshotHandler;
//import com.intergral.deep.agent.tracepoint.handler.VariableProcessor_old;
//import com.intergral.deep.agent.types.snapshot.StackFrame;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class CFSnapshotHandler extends SnapshotHandler
//{
//    public CFSnapshotHandler( final String filename,
//                              final int lineNo,
//                              final Map<String, Object> variables,
//                              final IEvaluator evaluator,
//                              final VariableProcessor_old variableProcessor )
//    {
//        super( filename, lineNo, variables, evaluator, variableProcessor );
//    }
//
//
//    @Override
//    protected String getMethodName( final StackTraceElement stackTraceElement, final Map<String, Object> variables, final int i )
//    {
//        final String udf = CFUtils.findUDFName( variables, stackTraceElement.getClassName(), i );
//        if( udf != null )
//        {
//            return udf;
//        }
//        else
//        {
//            return stackTraceElement.getMethodName();
//        }
//    }
//
//
//    /**
//     * When processing a CF snapshot we want to add the variables as seen by a CF dev, these are a set of scopes that are accessed
//     * via a context.
//     *
//     * @param variables the variables
//     *
//     * @return the variables modified to the scope set
//     */
//    Map<String, Object> convertScopes( final Map<String, Object> variables )
//    {
//        final Object aThis = variables.get( "this" );
//        if( CFUtils.isLucee( aThis ) )
//        {
//            // lucee requires special handling
//            return convertLuceeScopes( variables );
//        }
//
//        // handle local scope
//        final Map<String, Object> cfVars = new HashMap<>();
//        final Object localScope = variables.get( "__localScope" );
//
//        if( CFUtils.isScope( localScope ) )
//        {
//            final Map<Object, Object> lclMap = (Map) localScope;
//            for( Map.Entry<Object, Object> entry : lclMap.entrySet() )
//            {
//                final Object name = entry.getKey();
//                final Object value = entry.getValue();
//                cfVars.put( Utils.valueOf( name ), value );
//            }
//        }
//
//        // other scopes are on the context to find that
//        final Object pageContext = CFUtils.findPageContext( variables );
//        if( pageContext == null )
//        {
//            return cfVars;
//        }
//
//        // handle var scope
//        final Object varScope = ReflectionUtils.getReflection().getFieldValue( pageContext, "SymTab_varScope" );
//
//        if( CFUtils.isScope( varScope ) )
//        {
//            cfVars.put( "VARIABLES", varScope );
//        }
//
//        // find the other build in scopes
//        final Map<Object, Object> scopes = ReflectionUtils.getReflection().getFieldValue( pageContext, "SymTab_builtinCFScopes" );
//        if( scopes == null )
//        {
//            return cfVars;
//        }
//
//        // handle each of the scopes
//        for( Map.Entry<Object, Object> entry : scopes.entrySet() )
//        {
//            final Object name = entry.getKey();
//            final Object value = entry.getValue();
//
//            // cheap hack to check class is a CF scope
//            if( !CFUtils.isScope( value ) )
//            {
//                continue;
//            }
//            cfVars.put( Utils.valueOf( name ), value );
//        }
//
//        return cfVars;
//    }
//
//
//    @Override
//    protected boolean isCf()
//    {
//        return true;
//    }
//
//
//    /**
//     * Special handling for lucee scopes
//     *
//     * @param variables the variables
//     *
//     * @return the scopes for lucee
//     */
//    Map<String, Object> convertLuceeScopes( final Map<String, Object> variables )
//    {
//        // in lucee variable names are removed, but we know that 'param0' is the page context
//        final Object param0 = variables.get( "param0" );
//        if( param0 == null )
//        {
//            return new HashMap<>();
//        }
//
//        final Map<String, Object> scopes = new HashMap<>();
//        // process the scopes from lucee
//        scopes.put( "variables", ReflectionUtils.getReflection().getFieldValue( param0, "variables" ) );
//        scopes.put( "argument", ReflectionUtils.getReflection().getFieldValue( param0, "argument" ) );
//        scopes.put( "local", getAndCheckLocal( "local", param0 ) );
//        scopes.put( "cookie", getAndCheckScope( "cookie", param0 ) );
//        scopes.put( "server", ReflectionUtils.getReflection().getFieldValue( param0, "server" ) );
//        scopes.put( "session", getAndCheckScope( "session", param0 ) );
//        scopes.put( "application", ReflectionUtils.getReflection().getFieldValue( param0, "application" ) );
//        scopes.put( "cgi", getAndCheckScope( "cgiR", param0 ) );
//        scopes.put( "request", getAndCheckScope( "request", param0 ) );
//        scopes.put( "form", getAndCheckScope( "_form", param0 ) );
//        scopes.put( "url", getAndCheckScope( "_url", param0 ) );
//        scopes.put( "client", getAndCheckScope( "client", param0 ) );
//        scopes.put( "threads", getAndCheckScope( "threads", param0 ) );
//
//        return scopes;
//    }
//
//
//    /**
//     * This method will get anc check that the field is a local scope as some parts can have no local scope.
//     *
//     * @param local  the name of the field to look for
//     * @param param0 the object to look at
//     *
//     * @return {@code null} if the field is not a valid local scope
//     */
//    private Object getAndCheckLocal( final String local, final Object param0 )
//    {
//        final Object o = ReflectionUtils.getReflection().getFieldValue( param0, local );
//        if( o == null || o.getClass().getName().equals( "lucee.runtime.type.scope.LocalNotSupportedScope" ) )
//        {
//            return null;
//        }
//        return o;
//    }
//
//
//    /**
//     * This method loads and checks the scope for lucee, as it is possible that the scope is not initialised. If it is not then
//     * the scope can be in an invalidate state and should be ignored.
//     *
//     * @param name the name of the field to look for
//     * @param target the object to look for
//     *
//     * @return {@code null} if the field is not a scope or is not initialised, else the scope discovered.
//     */
//    private Object getAndCheckScope( final String name, final Object target )
//    {
//        final Object local = ReflectionUtils.getReflection().getFieldValue( target, name );
//        if( local != null )
//        {
//            final Class<?> aClass = local.getClass();
//            final Object isInitalized = ReflectionUtils.getReflection().callMethod( local, "isInitialized" );
//            if( isInitalized == null )
//            {
//                return null;
//            }
//
//            final boolean isInitalizedRtn = Boolean.parseBoolean( String.valueOf( isInitalized ) );
//            if( isInitalizedRtn )
//            {
//                return local;
//            }
//        }
//        return null;
//    }
//
//
//    @Override
//    public List<StackFrame> processVariables( final boolean processFrameVars,
//                                              final boolean processFrameStack,
//                                              final long lineStart )
//    {
//        if( !processFrameStack )
//        {
//            return Collections.emptyList();
//        }
//        return super.processFrames( convertScopes( this.variables ), processFrameVars, lineStart );
//    }
//}
