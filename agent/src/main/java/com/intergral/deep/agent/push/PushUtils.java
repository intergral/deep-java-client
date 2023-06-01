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

public class PushUtils {

  public static Snapshot convertToGrpc(final EventSnapshot snapshot) {
    return Snapshot.newBuilder()
        .setID(ByteString.copyFromUtf8(snapshot.getID()))
        .setTracepoint(convertTracepoint(snapshot.getTracepoint()))
        .putAllVarLookup(convertVarLookup(snapshot.getVarLookup()))
        .setTsNanos(snapshot.getNanoTs())
        .addAllFrames(convertFrames(snapshot.getFrames()))
        .addAllWatches(convertWatches(snapshot.getWatches()))
        .addAllAttributes(convertAttributes(snapshot.getAttributes()))
        .setDurationNanos(snapshot.getDurationNanos())
        .addAllResource(convertAttributes(snapshot.getResource()))
        .build();
  }

  private static Iterable<? extends KeyValue> convertAttributes(final Resource attributes) {
    return attributes.getAttributes().entrySet().stream()
        .map(stringObjectEntry -> KeyValue.newBuilder()
            .setKey(stringObjectEntry.getKey())
            .setValue(asAnyValue(stringObjectEntry.getValue()))
            .build()).collect(Collectors.toList());
  }

  private static AnyValue asAnyValue(final Object value) {
    final AnyValue.Builder builder = AnyValue.newBuilder();

    if (value instanceof String) {
      builder.setStringValue((String) value);
    } else if (value instanceof Integer) {
      builder.setIntValue((int) value);
    } else if (value instanceof Long) {
      builder.setIntValue((long) value);
    } else if (value instanceof Double) {
      builder.setDoubleValue((double) value);
    } else if (value instanceof Float) {
      builder.setDoubleValue((float) value);
    } else if (value instanceof Boolean) {
      builder.setBoolValue((Boolean) value);
    }
    //todo should we support all types

    return builder.build();
  }

  private static Iterable<? extends WatchResult> convertWatches(
      final ArrayList<com.intergral.deep.agent.types.snapshot.WatchResult> watches) {
    return watches.stream().map(watchResult -> {
      final WatchResult.Builder builder = WatchResult.newBuilder();
      builder.setExpression(watchResult.expression());
      if (watchResult.isError()) {
        builder.setErrorResult(watchResult.error());
      } else {
        builder.setGoodResult(convertVariableID(watchResult.goodResult()));
      }
      return builder.build();
    }).collect(Collectors.toList());
  }

  private static com.intergral.deep.proto.tracepoint.v1.VariableID convertVariableID(
      final VariableID variableId) {
    final com.intergral.deep.proto.tracepoint.v1.VariableID.Builder builder =
        com.intergral.deep.proto.tracepoint.v1.VariableID.newBuilder()
            .setID(variableId.getId())
            .setName(variableId.getName())
            .addAllModifiers(variableId.getModifiers());

    if (variableId.getOriginalName() != null) {
      builder.setOriginalName(variableId.getOriginalName());
    }
    return builder.build();
  }

  private static Iterable<? extends StackFrame> convertFrames(
      final Collection<com.intergral.deep.agent.types.snapshot.StackFrame> frames) {
    return frames.stream().map(stackFrame -> StackFrame.newBuilder()
        .setFileName(stackFrame.getFileName())
        .setMethodName(stackFrame.getMethodName())
        .setLineNumber(stackFrame.getLineNumber())
        .setClassName(stackFrame.getClassName())
//                    .setIsAsync( false )
//                    .setColumnNumber( 0 )
        //todo update for JSP/CFM
//                    .setTranspiledFileName( "" )
//                    .setTranspiledLineNumber( 0 )
//                    .setTranspiledColumnNumber( 0 )
        .addAllVariables(covertVariables(stackFrame.getFrameVariables()))
        .setAppFrame(stackFrame.isAppFrame())
        .build()).collect(Collectors.toList());
  }

  private static Iterable<? extends com.intergral.deep.proto.tracepoint.v1.VariableID> covertVariables(
      final Collection<VariableID> frameVariables) {
    return frameVariables.stream().map(PushUtils::convertVariableID).collect(Collectors.toList());
  }

  private static Map<String, Variable> convertVarLookup(
      final Map<String, com.intergral.deep.agent.types.snapshot.Variable> varLookup) {
    return varLookup.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            stringVariableEntry -> convertVariable(stringVariableEntry.getValue())));

  }

  private static Variable convertVariable(
      final com.intergral.deep.agent.types.snapshot.Variable value) {
    return Variable.newBuilder().setType(value.getVarType())
        .setValue(value.getValString())
        .setHash(value.getIdentityCode())
        .addAllChildren(covertVariables(value.getChildren()))
        .setTruncated(value.isTruncated()).build();
  }

  private static TracePointConfig convertTracepoint(
      final com.intergral.deep.agent.types.TracePointConfig tracepoint) {
    return TracePointConfig.newBuilder()
        .setID(tracepoint.getId())
        .setPath(tracepoint.getPath())
        .setLineNumber(tracepoint.getLineNo())
        .putAllArgs(tracepoint.getArgs())
        .addAllWatches(tracepoint.getWatches())
        .build();
  }
}
