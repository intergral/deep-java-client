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

package java.com.intergral.deep;

import com.intergral.deep.agent.tracepoint.handler.Callback;
import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * This type is here to allow us to access it from anywhere (once it is loaded into the boot class path).
 * <p>
 * This will simply act as a proxy to the {@link Callback} which is where we do the real work.
 * <p>
 * This split is to allow us to support Lucee and other OSGi style environments that use isolated class loaders.
 */
@SuppressWarnings("unused")
public class ProxyCallback {

  /**
   * The main entry point for CF ASM injected breakpoints.
   *
   * @param bpIds    the bp ids to trigger
   * @param filename the filename of the breakpoint hit
   * @param lineNo   the line number of the breakpoint hit
   * @param map      the map of local variables.
   */
  public static void callBackCF(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> map) {
    Callback.callBackCF(bpIds, filename, lineNo, map);
  }


  /**
   * The main entry point for non CF ASM injected breakpoints.
   *
   * @param bpIds    the bp ids to trigger
   * @param filename the filename of the breakpoint hit
   * @param lineNo   the line number of the breakpoint hit
   * @param map      the map of local variables.
   */
  public static void callBack(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> map) {
    Callback.callBack(bpIds, filename, lineNo, map);
  }


  /**
   * This is called when an exception is caught on a wrapped line, this is not always called.
   *
   * @param e the exception caught
   */
  public static void callBackException(final Throwable e) {
    Callback.callBackException(e);
  }


  /**
   * This is called when the visited line is completed.
   *
   * @param bpIds the tracepoint ids that triggered this
   * @param filename the source file name
   * @param lineNo the line number we are on
   * @param variables the captured local variables
   */
  public static void callBackFinally(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> variables) {
    Callback.callBackFinally(bpIds, filename, lineNo, variables);
  }

  /**
   * This method is called when a tracepoint has triggered a method entry type.
   * <p>
   * This method will <b>Always</b> return a closable. This way the injected code never deals with anything but calling close. Even if close
   * doesn't do anything.
   * <p>
   * We use {@link Closeable} here, so we can stick to java types in the injected code. This makes testing and injected code simpler.
   *
   * @param methodName the method name we have entered
   * @param filename   the file name the method is in
   * @param lineNo     the line number the method is on
   * @param bpIds      the tracepoint ids that have been triggered by this entry
   * @param variables  the map of variables captured
   * @param spanOnlyIds the CSV of the tracepoints ids that just want a span
   */
  public static void methodEntry(final String methodName, final String filename, final int lineNo, final List<String> bpIds,
      final Map<String, Object> variables, final String spanOnlyIds) {
    Callback.methodEntry(methodName, filename, lineNo, bpIds, variables, spanOnlyIds);
  }

  /**
   * This is called when an exception is captured from a wrapped method.
   *
   * @param t the captured throwable
   */
  public static void methodException(final Throwable t) {
    Callback.methodException(t);
  }

  /**
   * This is called when the returned value from the wrapped method is captured.
   * <p>
   * This method is not called on void methods.
   *
   * @param value the captured return value.
   */
  public static void methodRet(final Object value) {
    Callback.methodRet(value);
  }

  /**
   * This method is called when a wrapped method has completed.
   *
   * @param methodName the method name
   * @param filename the source file name
   * @param lineNo the line number
   * @param bpIds the triggering tracepoints ids
   * @param variables the captured local variables
   */
  public static void methodEnd(final String methodName, final String filename, final int lineNo, final List<String> bpIds,
      final Map<String, Object> variables) {
    Callback.methodEnd(methodName, filename, lineNo, bpIds, variables);
  }
}
