/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.evaluator.IEvaluator;
import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.StackFrame;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.agent.types.snapshot.WatchResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FrameCollector extends VariableProcessor
{
    protected final Settings settings;
    protected final IEvaluator evaluator;
    protected final Map<String, Object> variables;
    private final StackTraceElement[] stack;

    private final Map<String, String> varCache = new HashMap<>();

    public FrameCollector( final Settings settings, final IEvaluator evaluator, final Map<String, Object> variables,
                           final StackTraceElement[] stack )
    {
        this.settings = settings;
        this.evaluator = evaluator;
        this.variables = variables;
        this.stack = stack;
    }

    protected IFrameResult processFrame()
    {
        final ArrayList<StackFrame> frames = new ArrayList<>();

        for( int i = 0; i < stack.length; i++ )
        {
            final StackTraceElement stackTraceElement = stack[i];
            final StackFrame frame = this.processFrame( stackTraceElement, this.frameConfig.shouldCollectVars( i ), i );
            frames.add( frame );
        }

        return new IFrameResult()
        {
            @Override
            public Collection<StackFrame> frames()
            {
                return frames;
            }

            @Override
            public Map<String, Variable> variables()
            {
                final Map<String, Variable> lookup = FrameCollector.super.varLookup;
                if( lookup == null )
                {
                    return Collections.emptyMap();
                }
                return lookup;
            }
        };
    }

    private StackFrame processFrame( final StackTraceElement stackTraceElement, final boolean collectVars,
                                     final int frameIndex )
    {
        final String className = stackTraceElement.getClassName();
        final int lineNumber = stackTraceElement.getLineNumber();
        final String fileName = getFileName( stackTraceElement );
        final boolean nativeMethod = stackTraceElement.isNativeMethod();
        final boolean appFrame = isAppFrame( stackTraceElement );
        final String methodName = getMethodName( stackTraceElement, variables, frameIndex );


        final Collection<VariableID> varIds;
        if( collectVars )
        {
            varIds = this.processVars( this.selectVariables( frameIndex ) );
        }
        else
        {
            varIds = Collections.emptyList();
        }

        return new StackFrame( fileName,
                lineNumber,
                className,
                methodName,
                appFrame,
                nativeMethod,
                varIds );
    }

    private Map<String, Object> selectVariables( final int frameIndex )
    {
        if( frameIndex == 0 )
        {
            return this.variables;
        }
        // todo can we use native to get variables up the stack
        return Collections.emptyMap();
    }

    protected Collection<VariableID> processVars( final Map<String, Object> variables )
    {
        final List<VariableID> frameVars = new ArrayList<>();

        final Node.IParent frameParent = frameVars::add;

        final Set<Node> initialNodes = variables.entrySet()
                .stream()
                .map( stringObjectEntry -> new Node( new Node.NodeValue( stringObjectEntry.getKey(),
                        stringObjectEntry.getValue() ), frameParent ) ).collect( Collectors.toSet() );

        Node.breadthFirstSearch( new Node( null, new HashSet<>(initialNodes), frameParent ), this::processNode );

        return frameVars;
    }

    protected boolean processNode( final Node node )
    {
        if( !this.checkVarCount() )
        {
            // we have exceeded the var count, so do not continue
            return false;
        }

        final Node.NodeValue value = node.getValue();
        if( value == null )
        {
            // this node has no value, continue with children
            return true;
        }

        // process this node variable
        final VariableResponse processResult = super.processVariable( value );
        final VariableID variableId = processResult.getVariableId();

        // add the result to the parent - this maintains the hierarchy in the var look up
        node.getParent().addChild( variableId );

        if( processResult.processChildren() )
        {
            final Set<Node> childNodes = super.processChildNodes( variableId, value.getValue(), node.depth() );
            node.addChildren( childNodes );
        }
        return true;
    }

    private boolean checkVarCount()
    {
        return varCache.size() <= this.frameConfig.maxVariables();
    }

    @Override
    protected void appendChild( final String parentId, final VariableID variableId )
    {
        final Variable variable = this.varLookup.get( parentId );
        variable.addChild( variableId );
    }

    @Override
    protected void appendVariable( final String varId, final Variable variable )
    {
        this.varLookup.put( varId, variable );
    }

    @Override
    protected String checkId( final String identity )
    {
        return this.varCache.get( identity );
    }

    @Override
    protected String newVarId( final String identity )
    {
        final int size = this.varCache.size();
        final String newId = String.valueOf( size + 1 );
        this.varCache.put( identity, newId );
        return newId;
    }

    private boolean isAppFrame( final StackTraceElement stackTraceElement )
    {
        final List<String> inAppInclude = settings.getAsList( "in_app.include" );
        final List<String> inAppExclude = settings.getAsList( "in_app.exclude" );

        final String className = stackTraceElement.getClassName();

        for( String exclude : inAppExclude )
        {
            if( className.equals( exclude ) )
            {
                return false;
            }
        }

        for( String include : inAppInclude )
        {
            if( className.equals( include ) )
            {
                return true;
            }
        }
        return false;
    }

    private String getFileName( final StackTraceElement stackTraceElement )
    {
        if( stackTraceElement.getFileName() == null )
        {
            return InstUtils.shortClassName( stackTraceElement.getClassName() );
        }
        return stackTraceElement.getFileName();
    }

    private String getMethodName( final StackTraceElement stackTraceElement,
                                  final Map<String, Object> variables,
                                  final int frameIndex )
    {
        return stackTraceElement.getMethodName();
    }

    protected IExpressionResult evaluateExpression( final String watch )
    {
        return new IExpressionResult()
        {
            @Override
            public WatchResult result()
            {
                return null;
            }

            @Override
            public Map<String, Variable> variables()
            {
                return null;
            }
        };
    }

    protected Resource processAttributes( final TracePointConfig tracepoint )
    {
        final HashMap<String, Object> attributes = new HashMap<>();
        attributes.put( "tp", tracepoint.getId() );
        attributes.put( "path", tracepoint.getPath() );
        attributes.put( "line", tracepoint.getLineNo() );
        attributes.put( "stack", tracepoint.getStackType() );
        attributes.put( "frame", tracepoint.getFrameType() );

        if( !tracepoint.getWatches().isEmpty() )
        {
            attributes.put( "has_watches", true );
        }

        if( tracepoint.getCondition() != null && !tracepoint.getCondition().trim().isEmpty() )
        {
            attributes.put( "has_condition", true );
        }

        return Resource.create( attributes );
    }

    protected interface IFrameResult
    {
        Collection<StackFrame> frames();

        Map<String, Variable> variables();
    }

    protected interface IExpressionResult
    {
        WatchResult result();

        Map<String, Variable> variables();
    }
}
