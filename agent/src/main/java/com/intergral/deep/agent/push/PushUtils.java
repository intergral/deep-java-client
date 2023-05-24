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

package com.intergral.deep.agent.push;


import com.google.protobuf.ByteString;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.proto.common.v1.AnyValue;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.StackFrame;
import com.intergral.deep.proto.tracepoint.v1.TracePointConfig;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.proto.tracepoint.v1.WatchResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class PushUtils
{
    public static Snapshot convertToGrpc( final EventSnapshot snapshot )
    {
        return Snapshot.newBuilder()
                .setID( ByteString.copyFromUtf8( snapshot.getID() ) )
                .setTracepoint( convertTracepoint( snapshot.getTracepoint() ) )
                .putAllVarLookup( convertVarLookup( snapshot.getVarLookup() ) )
                .setTsNanos( snapshot.getNanoTs() )
                .addAllFrames( convertFrames( snapshot.getFrames() ) )
                .addAllWatches( convertWatches( snapshot.getWatches() ) )
                .addAllAttributes( convertAttributes( snapshot.getAttributes() ) )
                .setDurationNanos( snapshot.getDurationNanos() )
                .addAllResource( convertAttributes( snapshot.getResource() ) )
                .build();
    }

    private static Iterable<? extends KeyValue> convertAttributes( final Resource attributes )
    {
        return attributes.getAttributes().entrySet().stream().map( stringObjectEntry -> KeyValue.newBuilder()
                .setKey( stringObjectEntry.getKey() )
                .setValue( asAnyValue( stringObjectEntry.getValue() ) )
                .build() ).collect( Collectors.toList() );
    }

    private static AnyValue asAnyValue( final Object value )
    {
        final AnyValue.Builder builder = AnyValue.newBuilder();

        if( value instanceof String )
        {
            builder.setStringValue( (String) value );
        }
        else if( value instanceof Integer )
        {
            builder.setIntValue( (int) value );
        }
        else if( value instanceof Long )
        {
            builder.setIntValue( (long) value );
        }
        else if( value instanceof Double )
        {
            builder.setDoubleValue( (double) value );
        }
        else if( value instanceof Float )
        {
            builder.setDoubleValue( (float) value );
        }
        else if( value instanceof Boolean )
        {
            builder.setBoolValue( (Boolean) value );
        }
        //todo should we support all types

        return builder.build();
    }

    private static Iterable<? extends WatchResult> convertWatches(
            final ArrayList<com.intergral.deep.agent.types.snapshot.WatchResult> watches )
    {
        return watches.stream().map( watchResult -> {
            final WatchResult.Builder builder = WatchResult.newBuilder();
            if( watchResult.isError() )
            {
                builder.setErrorResult( watchResult.error() );
            }
            else
            {
                builder.setGoodResult( convertVariableID( watchResult.goodResult() ) );
            }
            return builder.build();
        } ).collect( Collectors.toList() );
    }

    private static com.intergral.deep.proto.tracepoint.v1.VariableID convertVariableID( final VariableID variableId )
    {
        final com.intergral.deep.proto.tracepoint.v1.VariableID.Builder builder = com.intergral.deep.proto.tracepoint.v1.VariableID.newBuilder()
                .setID( variableId.getId() )
                .setName( variableId.getName() )
                .addAllModifiers( variableId.getModifiers() );

        if( variableId.getOriginalName() != null )
        {
            builder.setOriginalName( variableId.getOriginalName() );
        }
        return builder.build();
    }

    private static Iterable<? extends StackFrame> convertFrames(
            final Collection<com.intergral.deep.agent.types.snapshot.StackFrame> frames )
    {
        return frames.stream().map( stackFrame -> {
            return StackFrame.newBuilder()
                    .setFileName( stackFrame.getFileName() )
                    .setMethodName( stackFrame.getMethodName() )
                    .setLineNumber( stackFrame.getLineNumber() )
                    .setClassName( stackFrame.getClassName() )
//                    .setIsAsync( false )
//                    .setColumnNumber( 0 )
                    //todo update for JSP/CFM
//                    .setTranspiledFileName( "" )
//                    .setTranspiledLineNumber( 0 )
//                    .setTranspiledColumnNumber( 0 )
                    .addAllVariables( covertVariables( stackFrame.getFrameVariables() ) )
                    .setAppFrame( stackFrame.isAppFrame() )
                    .build();
        } ).collect( Collectors.toList() );
    }

    private static Iterable<? extends com.intergral.deep.proto.tracepoint.v1.VariableID> covertVariables(
            final Collection<VariableID> frameVariables )
    {
        return frameVariables.stream().map( PushUtils::convertVariableID ).collect( Collectors.toList() );
    }

    private static Map<String, Variable> convertVarLookup(
            final Map<String, com.intergral.deep.agent.types.snapshot.Variable> varLookup )
    {
        return varLookup.entrySet()
                .stream()
                .collect( Collectors.toMap( Map.Entry::getKey,
                        stringVariableEntry -> convertVariable( stringVariableEntry.getValue() ) ) );

    }

    private static Variable convertVariable( final com.intergral.deep.agent.types.snapshot.Variable value )
    {
        return Variable.newBuilder().setType( value.getVarType() )
                .setValue( value.getValString() )
                .setHash( value.getIdentityCode() )
                .addAllChildren( covertVariables( value.getChildren() ) )
                .setTruncated( value.isTruncated() ).build();
    }

    private static TracePointConfig convertTracepoint(
            final com.intergral.deep.agent.types.TracePointConfig tracepoint )
    {
        return TracePointConfig.newBuilder()
                .setID( tracepoint.getId() )
                .setPath( tracepoint.getPath() )
                .setLineNumber( tracepoint.getLineNo() )
                .putAllArgs( tracepoint.getArgs() )
                .addAllWatches( tracepoint.getWatches() )
                .build();
    }
}
