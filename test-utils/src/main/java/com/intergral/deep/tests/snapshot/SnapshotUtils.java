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

package com.intergral.deep.tests.snapshot;

import com.intergral.deep.proto.common.v1.AnyValue;
import com.intergral.deep.proto.common.v1.ArrayValue;
import com.intergral.deep.proto.common.v1.KeyValue;
import com.intergral.deep.proto.common.v1.KeyValueList;
import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.proto.tracepoint.v1.VariableID;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SnapshotUtils {

  private SnapshotUtils() {
  }

  /**
   * Scan a snapshot for a variable with the given name.
   *
   * @param name     the variable name
   * @param snapshot the snapshot
   * @return a {@link IVariableScan}
   */
  public static IVariableScan findVarByName(final String name, final Snapshot snapshot) {
    return findVarByName(name, snapshot.getFrames(0).getVariablesList(), snapshot.getVarLookupMap());
  }

  /**
   * Scan a snapshot for a variable with a given name.
   *
   * @param name      the name to look for
   * @param localVars the variables to scan (e.g. the local frame, or the children of another var)
   * @param lookup    the lookup from the snapshot to get the var value from
   * @return a {@link IVariableScan}
   */
  public static IVariableScan findVarByName(final String name,
      final Collection<VariableID> localVars,
      final Map<String, Variable> lookup) {
    final Optional<VariableID> first = localVars.stream()
        .filter(variableID -> Objects.equals(variableID.getName(), name))
        .findFirst();
    if (!first.isPresent()) {
      return () -> false;
    }
    final VariableID variableId = first.get();
    final Variable variable = lookup.get(variableId.getID());
    return new IVariableScan() {
      @Override
      public boolean found() {
        return true;
      }

      @Override
      public Variable variable() {
        return variable;
      }

      @Override
      public VariableID variableId() {
        return variableId;
      }
    };
  }

  public static String attributeByName(final String name, final Snapshot snapshot) {
    final List<KeyValue> attributesList = snapshot.getAttributesList();
    for (KeyValue keyValue : attributesList) {
      if (keyValue.getKey().equals(name)) {
        return keyValueAsString(keyValue.getValue());
      }
    }
    return null;
  }

  private static String keyValueAsString(final AnyValue value) {
    if (value.hasArrayValue()) {
      final ArrayValue arrayValue = value.getArrayValue();
      final StringBuilder stringBuilder = new StringBuilder();
      for (AnyValue anyValue : arrayValue.getValuesList()) {
        stringBuilder.append(keyValueAsString(anyValue)).append(",");
      }
      return stringBuilder.toString();
    }
    if (value.hasStringValue()) {
      return value.getStringValue();
    }
    if (value.hasBoolValue()) {
      return String.valueOf(value.getBoolValue());
    }
    if (value.hasBytesValue()) {
      return String.valueOf(value.getBytesValue());
    }
    if (value.hasDoubleValue()) {
      return String.valueOf(value.getDoubleValue());
    }
    if (value.hasIntValue()) {
      return String.valueOf(value.getIntValue());
    }
    if (value.hasKvlistValue()) {
      final KeyValueList kvlistValue = value.getKvlistValue();
      final StringBuilder stringBuilder = new StringBuilder();
      for (KeyValue anyValue : kvlistValue.getValuesList()) {
        stringBuilder.append(anyValue.getKey()).append(":").append(keyValueAsString(anyValue.getValue())).append(",");
      }
      return stringBuilder.toString();
    }
    return null;
  }

  public interface IVariableScan {

    boolean found();

    default Variable variable() {
      return null;
    }

    default VariableID variableId() {
      return null;
    }
  }
}
