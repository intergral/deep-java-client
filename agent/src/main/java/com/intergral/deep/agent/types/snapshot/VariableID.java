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

package com.intergral.deep.agent.types.snapshot;

import java.util.Set;

/**
 * A VariableID is the reference to a variable in the snapshot variable lookup.
 */
public class VariableID {

  private final String id;
  private final String name;
  private final Set<String> modifiers;
  private final String originalName;

  /**
   * Create a new variable ID.
   *
   * @param id the variable id
   * @param name the variable name
   * @param modifiers the modifiers
   * @param originalName the orignal name
   */
  public VariableID(final String id, final String name, final Set<String> modifiers,
      final String originalName) {

    this.id = id;
    this.name = name;
    this.modifiers = modifiers;
    this.originalName = originalName;
  }

  public String getId() {
    return this.id;
  }

  public Iterable<String> getModifiers() {
    return modifiers;
  }

  public String getName() {
    return name;
  }

  public String getOriginalName() {
    return originalName;
  }
}
