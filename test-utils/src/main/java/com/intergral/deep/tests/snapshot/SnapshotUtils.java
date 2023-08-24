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

import com.intergral.deep.proto.tracepoint.v1.Snapshot;
import com.intergral.deep.proto.tracepoint.v1.Variable;
import com.intergral.deep.proto.tracepoint.v1.VariableID;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SnapshotUtils {

  /**
   * Scan a snapshot for a variable with the given name
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
      final List<VariableID> localVars,
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
