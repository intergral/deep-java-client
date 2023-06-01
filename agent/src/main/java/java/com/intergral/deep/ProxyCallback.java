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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyCallback {

  /**
   * The main entry point for CF ASM injected breakpoints
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
   * The main entry point for non CF ASM injected breakpoints
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
   * This is called when a tracepoint 'finally' wrap is called.
   *
   * @param breakpointIds the ids for the tracepoints that are complete
   * @param map           the variables at this point
   */
  public static void callBackFinally(final Set<String> breakpointIds,
      final Map<String, Object> map) {
    Callback.callBackFinally(breakpointIds, map);
  }
}
