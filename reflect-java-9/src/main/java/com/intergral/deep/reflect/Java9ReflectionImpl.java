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

package com.intergral.deep.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Java9ReflectionImpl extends ReflectionImpl {

  @Override
  public boolean setAccessible(final Class<?> clazz, final Field field) {
    try {
      openModule(clazz);

      field.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  @Override
  public boolean setAccessible(final Class<?> clazz, final Method method) {
    try {
      openModule(clazz);

      method.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  @Override
  public boolean setAccessible(final Class<?> clazz, final Constructor<?> constructor) {
    try {
      openModule(clazz);

      constructor.setAccessible(true);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }


  private void openModule(final Class<?> clazz) {
    final Module m = clazz.getModule();
    if (m.isNamed()) {
      final String pkgName = clazz.getPackageName();
      m.addOpens(pkgName, getClass().getModule());
    }
  }
}
