/*
 *     Copyright (C) 2023  Intergral GmbH
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.cf.CFFrameProcessor;
import com.intergral.deep.agent.tracepoint.cf.CFUtils;
import com.intergral.deep.agent.tracepoint.evaluator.EvaluatorService;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.tracepoint.inst.asm.Visitor;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Callback
{
    @SuppressWarnings("AnonymousHasLambdaAlternative")
//    public static final ThreadLocal<Deque<CallbackHook>> CALLBACKS = new ThreadLocal<Deque<CallbackHook>>()
//    {
//        @Override
//        protected Deque<CallbackHook> initialValue()
//        {
//            return new ArrayDeque<>();
//        }
//    };
    private static final Logger LOGGER = LoggerFactory.getLogger( Callback.class );
    @SuppressWarnings("AnonymousHasLambdaAlternative")
    private static final ThreadLocal<Boolean> FIRING = new ThreadLocal<Boolean>()
    {
        @Override
        protected Boolean initialValue()
        {
            return Boolean.FALSE;
        }
    };
    private static Settings SETTINGS;

    private static TracepointConfigService BREAKPOINT_SERVICE;
    private static PushService PUSH_SERVICE;
    private static int offset;

    public static void init( final Settings settings,
                             final TracepointConfigService breakpointService,
                             final PushService pushService )
    {
        Callback.SETTINGS = settings;
        Callback.BREAKPOINT_SERVICE = breakpointService;
        Callback.PUSH_SERVICE = pushService;
        if( Visitor.CALLBACK_CLASS == Callback.class )
        {
            offset = 3;
        }
        else
        {
            offset = 4;
        }
    }


    /**
     * The main entry point for CF ASM injected breakpoints
     *
     * @param bpIds     the bp ids to trigger
     * @param filename  the filename of the breakpoint hit
     * @param lineNo    the line number of the breakpoint hit
     * @param variables the map of local variables.
     */
    public static void callBackCF( final List<String> bpIds,
                                   final String filename,
                                   final int lineNo,
                                   final Map<String, Object> variables )
    {
        try
        {
            final IEvaluator evaluator = CFUtils.findCfEval( variables );
            commonCallback( bpIds, filename, lineNo, variables, evaluator, CFFrameProcessor::new );
        }
        catch( Throwable t )
        {
            LOGGER.debug( "Unable to process tracepoint {}:{}", filename, lineNo, t );
        }
    }


    /**
     * The main entry point for non CF ASM injected breakpoints
     *
     * @param bpIds     the bp ids to trigger
     * @param filename  the filename of the breakpoint hit
     * @param lineNo    the line number of the breakpoint hit
     * @param variables the map of local variables.
     */
    public static void callBack( final List<String> bpIds,
                                 final String filename,
                                 final int lineNo,
                                 final Map<String, Object> variables )
    {
        try
        {
            final IEvaluator evaluator = EvaluatorService.createEvaluator();
            commonCallback( bpIds, filename, lineNo, variables, evaluator, FrameProcessor::new );
        }
        catch( Throwable t )
        {
            LOGGER.debug( "Unable to process tracepoint {}:{}", filename, lineNo, t );
        }
    }


    private static void commonCallback( final List<String> tracepointIds,
                                        final String filename,
                                        final int lineNo,
                                        final Map<String, Object> variables,
                                        final IEvaluator evaluator, final FrameProcessor.IFactory factory )
    {
        final long[] lineStart = Utils.currentTimeNanos();
        if( FIRING.get() )
        {
            LOGGER.debug( "hit - skipping as we are already firing" );
            return;
        }

        try
        {
            FIRING.set( true );
            LOGGER.trace( "callBack for {}:{} -> {}", filename, lineNo, tracepointIds );

            // possible race condition but unlikely
            if( Callback.BREAKPOINT_SERVICE == null )
            {
                return;
            }

            final Collection<TracePointConfig> tracePointConfigs = Callback.BREAKPOINT_SERVICE.loadTracepointConfigs(
                    tracepointIds );

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            if( stack.length > offset )
            {
                // Remove callBackProxy() + callBack() + commonCallback() + getStackTrace() entries to get to the real bp location
                stack = Arrays.copyOfRange( stack, offset, stack.length );
            }

            final FrameProcessor frameProcessor = factory.provide( Callback.SETTINGS,
                    evaluator,
                    variables,
                    tracePointConfigs,
                    lineStart,
                    stack );

            if( frameProcessor.canCollect() )
            {
                frameProcessor.configureSelf();

                try
                {
                    final Collection<EventSnapshot> snapshots = frameProcessor.collect();
                    for( EventSnapshot snapshot : snapshots )
                    {
                        Callback.PUSH_SERVICE.pushSnapshot( snapshot, frameProcessor );
                    }
                }
                catch( Exception e )
                {
                    LOGGER.debug( "Error processing snapshot", e );
                }
            }
        }
        finally
        {
            FIRING.set( false );
        }
    }


    public static void callBackException( final Throwable t )
    {
        System.out.println( "callBackException" );
//        try
//        {
//            LOGGER.debug( "Capturing throwable", t );
//            final CallbackHook element = CALLBACKS.get().peekLast();
//            if( element != null && element.isHook())
//            {
//                element.setThrowable( t );
//            }
//        }
//        catch( Throwable tt )
//        {
//            LOGGER.debug( "Error processing callback", tt );
//        }

    }


    public static void callBackFinally( final Set<String> breakpointIds, final Map<String, Object> map )
    {
        System.out.println( "callBackFinally" );
//        for( String breakpointId : breakpointIds )
//        {
//            try
//            {
//                LOGGER.debug( "{}: Processing finally", breakpointId );
//                final Deque<CallbackHook> hooks = CALLBACKS.get();
//                final CallbackHook pop = hooks.pollLast();
//                LOGGER.debug( "Dequeue state: {}", hooks );
//                if( pop == null || !pop.isHook() )
//                {
//                    LOGGER.debug( "No callback pending. {}", pop );
//                    continue;
//                }
//
//                final boolean processFrameStack = (pop.value.getType().equals( ITracepoint.STACK_TYPE )) || pop.value.getType()
//                        .equals( ITracepoint.FRAME_TYPE ) || pop.value.getType().equals( ITracepoint.LOG_POINT_TYPE )
//                        || pop.value.getType().equals( ITracepoint.NO_FRAME_TYPE );
//
//                final List<IStackFrame> frames;
//                if( pop.value.getArg( ITracepoint.LINE_HOOK_ARG_KEY ).equals( ITracepoint.LINE_HOOK_DATA_RIGHT ) )
//                {
//                    frames = pop.snapshotHandler.processFrames( map, processFrameStack, System.currentTimeMillis() );
//                }
//                else
//                {
//                    frames = pop.frames;
//                }
//
//                // trim frames to our type
//                @SuppressWarnings({ "RedundantTypeArguments", "Convert2Diamond" })
//                final IRequestDecorator iRequestDecorator = pop.snapshotHandler.generateSnapshotData( pop.watchValues, pop.value,
//                        frames, Collections.<String>emptySet(),
//                        NVError.fromThrowable( pop.throwable, new HashMap<String, String>() ),
//                        System.currentTimeMillis(), Callback.CLIENT_CONFIG.getTags() );
//
//                final EventSnapshot eventSnapshot = iRequestDecorator.getBody();
//                addDynamicTags( pop.value, eventSnapshot );
//
//                sendEvent( iRequestDecorator, eventSnapshot );
//            }
//            catch( Throwable t )
//            {
//                LOGGER.debug( "Error processing callback", t );
//            }
//        }
    }


//    public static class CallbackHook
//    {
//
//        private final SnapshotHandler snapshotHandler;
//        private final List<IWatcherResult> watchValues;
//        private final ITracepoint value;
//        private final List<IStackFrame> frames;
//        private Throwable throwable;
//
//
//        public CallbackHook( final SnapshotHandler snapshotHandler,
//                             final List<IWatcherResult> watchValues,
//                             final ITracepoint value,
//                             final List<IStackFrame> frames )
//        {
//            this.snapshotHandler = snapshotHandler;
//            this.watchValues = watchValues;
//            this.value = value;
//            this.frames = frames;
//        }
//
//
//        public CallbackHook()
//        {
//            this.snapshotHandler = null;
//            this.watchValues = null;
//            this.value = null;
//            this.frames = null;
//        }
//
//
//        /**
//         * This will return {@code true} when there is a live hook for this.
//         *
//         * @return {@code true} if this is a hook else {@code false}.
//         */
//        public boolean isHook()
//        {
//            return this.snapshotHandler != null;
//        }
//
//
//        void setThrowable( final Throwable t )
//        {
//            this.throwable = t;
//        }
//
//
//        @Override
//        public String toString()
//        {
//            if( value == null )
//            {
//                return "Marker for non callback";
//            }
//            return String.format( "%s:%s", value.getRelPath(), value.getLineNo() );
//        }
//    }
}
