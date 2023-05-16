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

package com.intergral.deep.api;

import java.util.Map;

/**
 * This is how Deep is to be loaded, default provider is in the 'deep' module.
 */
public interface IDeepLoader
{
    /**
     * Load the Deep agent into the provided process id.
     *
     * @param pid    the current process id
     * @param config the config to use
     * @throws Throwable if loader fails
     */
    void load( final String pid, final Map<String, Object> config ) throws Throwable;
}
