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

package com.intergral.deep.agent.poll;

public interface ITimerTask
{
    /**
     * This method is called by the {@link DriftAwareThread} at the end of each interval
     *
     * @param now the current time
     *
     * @throws Exception
     */
    void run( final long now ) throws Exception;


    /**
     * This method is called after the {@link #run(long)} method to allow performance tracking.
     *
     * @param duration the duration of the last execution
     * @param nextExecutionTime the next calculated execution time
     *
     * @return the modified execution next time
     *
     * @throws Exception
     */
    long callback( final long duration, final long nextExecutionTime ) throws Exception;
}
