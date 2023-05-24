/*
 *    Copyright 2023 Intergral GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package java.com.intergral.deep;

import com.intergral.deep.agent.tracepoint.handler.Callback;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyCallback
{

    /**
     * The main entry point for CF ASM injected breakpoints
     *
     * @param bpIds    the bp ids to trigger
     * @param filename the filename of the breakpoint hit
     * @param lineNo   the line number of the breakpoint hit
     * @param map      the map of local variables.
     */
    public static void callBackCF( final List<String> bpIds,
                                   final String filename,
                                   final int lineNo,
                                   final Map<String, Object> map )
    {
        Callback.callBackCF( bpIds, filename, lineNo, map );
    }


    /**
     * The main entry point for non CF ASM injected breakpoints
     *
     * @param bpIds    the bp ids to trigger
     * @param filename the filename of the breakpoint hit
     * @param lineNo   the line number of the breakpoint hit
     * @param map      the map of local variables.
     */
    public static void callBack( final List<String> bpIds,
                                 final String filename,
                                 final int lineNo,
                                 final Map<String, Object> map )
    {
        Callback.callBack( bpIds, filename, lineNo, map );
    }


    /**
     * This is called when an exception is caught on a wrapped line, this is not always called.
     *
     * @param e the exception caught
     */
    public static void callBackException( final Throwable e )
    {
        Callback.callBackException( e );
    }


    /**
     * This is called when a tracepoint 'finally' wrap is called.
     *
     * @param breakpointIds the ids for the tracepoints that are complete
     * @param map the variables at this point
     */
    public static void callBackFinally( final Set<String> breakpointIds, final Map<String, Object> map )
    {
        Callback.callBackFinally( breakpointIds, map );
    }
}
