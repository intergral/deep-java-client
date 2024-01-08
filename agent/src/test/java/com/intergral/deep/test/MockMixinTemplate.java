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

package com.intergral.deep.test;

import com.intergral.deep.agent.tracepoint.handler.Callback;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This type is used as a template for generating the ASM code used in the {@link com.intergral.deep.agent.tracepoint.inst.asm.Visitor}.
 * <p>
 * We use the ASM plugin for idea to simplify this process <a href="https://plugins.jetbrains.com/plugin/23368-asm-viewer">ASM Viewer</a>.
 */
public class MockMixinTemplate {

  public void $deep$voidTemplate() {
  }

  public void $deep$voidTemplate(final String arg1) {
  }

  public void voidTemplate() {
    Callback.methodEntry("voidTemplate", "MockMixinTemplate.java", 40, new ArrayList<>(), new HashMap<>(), "");
    try {
      $deep$voidTemplate();
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("voidTemplate", "MockMixinTemplate.java", 40, new ArrayList<>(), new HashMap<>());
    }
  }

  public void voidTemplate(final String arg1) {
    Callback.methodEntry("voidTemplate", "MockMixinTemplate.java", 40, new ArrayList<>(), new HashMap<>(), "");
    try {
      $deep$voidTemplate(arg1);
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("voidTemplate", "MockMixinTemplate.java", 40, new ArrayList<>(), new HashMap<>());
    }
  }

  public int $deep$intTemplate() {
    return -1;
  }

  public int $deep$intTemplate(final String arg1) {
    return -1;
  }

  public int intTemplate() {
    Callback.methodEntry("intTemplate", "MockMixinTemplate.java", 78, new ArrayList<>(), new HashMap<>(), "");
    try {
      final int i = $deep$intTemplate();
      Callback.methodRet(i);
      return i;
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("intTemplate", "MockMixinTemplate.java", 78, new ArrayList<>(), new HashMap<>());
    }
  }


  public int intTemplate(final String arg1) {
    Callback.methodEntry("intTemplate", "MockMixinTemplate.java", 94, new ArrayList<>(), new HashMap<>(), "");
    try {
      final int i = $deep$intTemplate(arg1);
      Callback.methodRet(i);
      return i;
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("intTemplate", "MockMixinTemplate.java", 94, new ArrayList<>(), new HashMap<>());
    }
  }

  public Object $deep$objectTemplate() {
    return null;
  }

  public Object $deep$objectTemplate(final String arg1) {
    return null;
  }

  public Object objectTemplate() {
    Callback.methodEntry("objectTemplate", "MockMixinTemplate.java", 117, new ArrayList<>(), new HashMap<>(), "");
    try {
      final Object o = $deep$objectTemplate();
      Callback.methodRet(o);
      return o;
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("objectTemplate", "MockMixinTemplate.java", 117, new ArrayList<>(), new HashMap<>());
    }
  }


  public Object objectTemplate(final String arg1) {
    Callback.methodEntry("objectTemplate", "MockMixinTemplate.java", 133, new ArrayList<>(), new HashMap<>(), "");
    try {
      final Object o = $deep$objectTemplate(arg1);
      Callback.methodRet(o);
      return o;
    } catch (Throwable t) {
      Callback.methodException(t);
      throw t;
    } finally {
      Callback.methodEnd("objectTemplate", "MockMixinTemplate.java", 133, new ArrayList<>(), new HashMap<>());
    }
  }
}
