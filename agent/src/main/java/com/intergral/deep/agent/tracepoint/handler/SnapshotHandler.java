//package com.intergral.deep.agent.tracepoint.handler;
//
//import com.intergral.deep.agent.tracepoint.cf.CFUtils;
//import com.intergral.deep.agent.tracepoint.evaluator.IEvaluator;
//import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
//import com.intergral.deep.agent.tracepoint.inst.InstUtils;
//import com.intergral.deep.agent.types.TracePointConfig;
//import com.intergral.deep.agent.types.snapshot.StackFrame;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class SnapshotHandler
//{
//    private static final Logger LOGGER = LoggerFactory.getLogger( SnapshotHandler.class );
//
//    protected final String filename;
//    protected final int lineNo;
//    protected final Map<String, Object> variables;
//    protected final VariableProcessor_old variableProcessor;
//    protected final IEvaluator evaluator;
//    protected final Map<Integer, Integer> variableCache = new HashMap<>();
//    private final Map<String, Map<String, Object>> varLookup = new HashMap<>();
//    private final long startTime = System.currentTimeMillis();
//    protected StackTraceElement[] stack;
//    protected SnapshotConfig config;
//    private int variableCount = 0;
//    private boolean timeExceeded = false;
//
//    public SnapshotHandler( final String filename,
//                            final int lineNo,
//                            final Map<String, Object> variables,
//                            final IEvaluator evaluator,
//                            final VariableProcessor_old variableProcessor )
//    {
//        this.evaluator = evaluator;
//        this.filename = filename;
//        this.lineNo = lineNo;
//        this.variables = variables;
//        this.variableProcessor = variableProcessor;
//    }
//
////
////    public void setStack( final StackTraceElement[] stack )
////    {
////        this.stack = stack;
////    }
////
////    public boolean checkCondition( final TracePointConfig value )
////    {
////        final String condition = value.getCondition();
////        LOGGER.debug( "checkCondition {}", condition );
////        if( condition == null || condition.isEmpty() )
////        {
////            return true;
////        }
////
////        final IEvaluator eval = loadEvaluator();
////        return eval.evaluate( condition, this.variables );
////    }
////
////    private IEvaluator loadEvaluator()
////    {
////        return this.evaluator;
////    }
////
////    public List<IWatcherResult> processWatchers( final IBreakpoint value )
////    {
////        final List<IWatcherResult> watchers = value.getWatchers();
////        if( watchers.isEmpty() )
////        {
////            return watchers;
////        }
////
////        final IEvaluator evaluator = loadEvaluator();
////        for( IWatcherResult watcher : watchers )
////        {
////            final Object result = evaluator.evaluateExpression( watcher.getExpression(), this.variables );
////            final Map<String, Object> processResult = variableProcessor.process( this, watcher.getName(), result,
////                    clientConfig.blackList() ).result();
////            ((WatchValue) watcher).setResult( processResult );
////        }
////        return watchers;
////    }
////
////    public IRequestDecorator generateSnapshotData( final List<IWatcherResult> watchValues,
////                                                   final ITracepoint currentBreakpoint,
////                                                   final List<IStackFrame> frames,
////                                                   final Set<String> flags,
////                                                   final INVError error,
////                                                   final long endTs,
////                                                   final Map<String, String> tags )
////    {
////        final String logMessage = processLogMessage( loadEvaluator(), watchValues, currentBreakpoint.getLogMessage() );
////
////        final String id = generateId();
////
////        return new IRequestDecorator()
////        {
////            @Override
////            public EventSnapshot getBody()
////            {
////                return new EventSnapshot( id,
////                        startTime,
////                        currentBreakpoint,
////                        watchValues,
////                        varLookup,
////                        frames,
////                        logMessage,
////                        flags,
////                        error,
////                        endTs,
////                        tags );
////            }
////
////
////            @Override
////            public String getQueryString()
////            {
////                return String.format( "breakpoint_id=%s&workspace_id=%s", currentBreakpoint.getId(),
////                        currentBreakpoint.getWorkspaceId() );
////            }
////        };
////    }
////
////    private String generateId()
////    {
////        return UUID.randomUUID().toString();
////    }
////
////    public Set<String> flags( final Set<String> flags )
////    {
////        final Set<String> nFlags = new HashSet<>( flags );
////        if( this.timeExceeded )
////        {
////            nFlags.add( "time_exceeded" );
////        }
////        if( this.variableCount > this.config.getMaxVariables() )
////        {
////            nFlags.add( "vars_exceeded" );
////        }
////        return nFlags;
////    }
//
//    protected List<StackFrame> processFrames( final Map<String, Object> variables,
//                                              final boolean processFrameVars,
//                                              final long lineStart )
//    {
//        final List<StackFrame> frameList = new LinkedList<>();
//        for( int i = 0; i < stack.length; i++ )
//        {
//            if( checkTimeExceeded( lineStart ) )
//            {
//                break;
//            }
//
//            final StackTraceElement stackTraceElement = stack[i];
//
//            final String className = stackTraceElement.getClassName();
//            final int lineNumber = stackTraceElement.getLineNumber();
//            final String fileName = stackTraceElement.getFileName();
//            String fileOverride = null;
//            boolean hidden = false;
//
//            if( stackTraceElement.isNativeMethod() )
//            {
//                fileOverride = "native call";
//            }
//            else if( fileName == null )
//            {
//                fileOverride = InstUtils.shortClassName( className );
//            }
//
//            if( isCf() && !CFUtils.isCFFile( fileName ) )
//            {
//                hidden = true;
//            }
//
//            final String methodName = getMethodName( stackTraceElement, variables, i );
//
//            final List<Map<String, Object>> frameVariables;
//            if( i == 0 && processFrameVars )
//            {
//                final List<Map<String, Object>> frameVars = new ArrayList<>();
//                final HashSet<Node> nodes = new HashSet<>();
//                final Node.IParent parent = new Node.IParent()
//                {
//                    @Override
//                    public void addChild( final Map<String, Object> child )
//                    {
//                        frameVars.add( child );
//                    }
//
//
//                    @Override
//                    public void flag( final String flag )
//                    {
//                        // dont care about flags here as we are the root
//                    }
//                };
//                for( final Map.Entry<String, Object> entry : variables.entrySet() )
//                {
//                    final Node node = new Node( new Node.IVarAccessor()
//                    {
//                        @Override
//                        public String name()
//                        {
//                            return entry.getKey();
//                        }
//
//
//                        @Override
//                        public Object value()
//                        {
//                            return entry.getValue();
//                        }
//                    } );
//                    node.setParent( parent );
//                    nodes.add( node );
//                }
//
//                final Node root = new Node( null, nodes );
//                root.setParent( parent );
//
//                //noinspection Convert2Lambda
//                Node.breadthFirstSearch( root, new Node.IConsumer()
//                {
//                    @Override
//                    public boolean processNode( final Node node )
//                    {
//                        if( !SnapshotHandler.this.checkCount() )
//                        {
//                            return false;
//                        }
//                        final Node.IVarAccessor entry = node.getValue();
//                        if( entry == null )
//                        {
//                            return true;
//                        }
//                        final Object value = entry.value();
//                        final VariableProcessor_old.IProcessResponse processResponse = variableProcessor.process(
//                                SnapshotHandler.this, entry.name(), value );
//                        final Map<String, Object> process = processResponse.result();
//                        node.getParent().addChild( process );
//
//                        if( processResponse.processChildren() )
//                        {
//                            final Set<Node> childNodes = variableProcessor.processChildNodes( SnapshotHandler.this,
//                                    (int) process.get( "id" ), value, node.depth() );
//                            node.addChildren( childNodes );
//                        }
//                        return true;
//                    }
//                } );
//                frameVariables = frameVars;
//            }
//            else
//            {
//                frameVariables = null;
//            }
////            frameList.add(
////                    new StackFrame( className,
////                            fileName,
////                            fileOverride,
////                            methodName,
////                            lineNumber,
////                            hidden,
////                            frameVariables ) );
//        }
//        return frameList;
//    }
//
//    private boolean checkTimeExceeded( final long lineStart )
//    {
//        final long duration = System.currentTimeMillis() - lineStart;
//        this.timeExceeded = duration > config.getMaxTpProcessTime();
//        return this.timeExceeded;
//    }
//
//    protected boolean isCf()
//    {
//        return false;
//    }
//
//    private boolean checkCount()
//    {
//        return this.variableCount <= this.config.getMaxVariables();
//    }
//
//    protected String getMethodName( final StackTraceElement stackTraceElement,
//                                    final Map<String, Object> variables,
//                                    final int finalI )
//    {
//        return stackTraceElement.getMethodName();
//    }
//
//    public Integer checkId( final int identityHashCode )
//    {
//        return this.variableCache.get( identityHashCode );
//    }
//
//    public Integer checkSizeAndNewId( final int identityHashCode )
//    {
//        final int size = this.variableCache.size();
//        final int newId = size + 1;
//        this.variableCache.put( identityHashCode, newId );
//        return newId;
//    }
//
//    public void count()
//    {
//        this.variableCount++;
//    }
//
//    public SnapshotConfig getConfig()
//    {
//        return config;
//    }
//
//    public void setConfig( final SnapshotConfig config )
//    {
//        this.config = config;
//        this.variableProcessor.setConfig( config );
//    }
//
//    public void updateVarLookup( final Integer id, final Map<String, Object> varValue )
//    {
//        this.varLookup.put( String.valueOf( id ), varValue );
//    }
//
//
//    public Map<String, Map<String, Object>> getVarLookup()
//    {
//        return varLookup;
//    }
//
//
//    public List<StackFrame> processVariables( final boolean processFrameVars,
//                                              final boolean processFrameStack,
//                                              final long lineStart )
//    {
//        if( !processFrameStack )
//        {
//            return Collections.emptyList();
//        }
//        return processFrames( this.variables, processFrameVars, lineStart );
//    }
//
//    public boolean conditionPasses( TracePointConfig tracePointConfig )
//    {
//        final String condition = tracePointConfig.getCondition();
//        // no condition so allow
//        if( condition == null || condition.trim().isEmpty() )
//        {
//            return true;
//        }
//
//        return this.evaluator.evaluate( condition, this.variables );
//    }
//}
