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

import com.intergral.deep.agent.tracepoint.handler.Callback;
import java.io.Closeable;
import java.io.IOException;

/**
 * This type is used as a template for generating the ASM code used in the {@link com.intergral.deep.agent.tracepoint.inst.asm.Visitor}.
 * <p>
 * We use the ASM plugin for idea to simplify this process <a href="https://plugins.jetbrains.com/plugin/23368-asm-viewer">ASM Viewer</a>.
 */
public class MixinTemplate {

  public void $deep$voidTemplate() {
  }

  public void voidTemplate() {
    final Closeable closeable = Callback.span("voidTemplate");
    try {
      $deep$voidTemplate();
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }

  public int $deep$intTemplate() {
    return -1;
  }

  public int intTemplate() {
    final Closeable closeable = Callback.span("intTemplate");
    try {
      return $deep$intTemplate();
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }

  public Object $deep$objectTemplate() {
    return null;
  }

  public Object objectTemplate() {
    final Closeable closeable = Callback.span("objectTemplate");
    try {
      return $deep$intTemplate();
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }

  public void $deep$voidTemplate(final String arg1) {
  }

  public void voidTemplate(final String arg1) {
    final Closeable closeable = Callback.span("voidTemplate");
    try {
      $deep$voidTemplate(arg1);
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }

  public int $deep$intTemplate(final String arg1) {
    return -1;
  }

  public int intTemplate(final String arg1) {
    final Closeable closeable = Callback.span("intTemplate");
    try {
      return $deep$intTemplate(arg1);
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }

  public Object $deep$objectTemplate(final String arg1) {
    return null;
  }

  public Object objectTemplate(final String arg1) {
    final Closeable closeable = Callback.span("objectTemplate");
    try {
      return $deep$intTemplate(arg1);
    } finally {
      try {
        if (closeable != null) {
          closeable.close();
        }
      } catch (IOException ignored) {
        // we ignore this exception
      }
    }
  }
}
