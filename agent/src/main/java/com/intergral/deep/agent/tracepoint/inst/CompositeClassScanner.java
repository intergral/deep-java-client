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

package com.intergral.deep.agent.tracepoint.inst;


import com.intergral.deep.agent.tracepoint.inst.asm.TransformerUtils;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompositeClassScanner implements IClassScanner {

  private final Set<IClassScanner> scanner = new HashSet<>();


  public void addScanner(final IClassScanner classScanner) {
    this.scanner.add(classScanner);
  }


  @Override
  public boolean scanClass(final Class<?> clazz) {
    for (IClassScanner classScanner : scanner) {
      if (classScanner.scanClass(clazz)) {
        return true;
      }
    }
    return false;
  }


  public Class<?>[] scanAll(final Instrumentation inst) {
    final List<Class<?>> classes = new ArrayList<>();
    final Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
    for (Class<?> allLoadedClass : allLoadedClasses) {
      if (!TransformerUtils.isExcludedClass(allLoadedClass)
          && inst.isModifiableClass(allLoadedClass)
          && scanClass(allLoadedClass)) {
        classes.add(allLoadedClass);
      }
    }
    return classes.toArray(new Class<?>[0]);
  }
}
