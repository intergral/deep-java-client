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

package com.intergral.deep.agent.resource;

import com.intergral.deep.agent.api.spi.Ordered;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class SpiUtil
{

    static <T extends Ordered> List<T> loadOrdered( Class<T> spiClass, ClassLoader serviceClassLoader )
    {
        return loadOrdered( spiClass, serviceClassLoader, ServiceLoader::load );
    }

    // VisibleForTesting
    static <T extends Ordered> List<T> loadOrdered(
            Class<T> spiClass, ClassLoader serviceClassLoader, ServiceLoaderFinder serviceLoaderFinder )
    {
        List<T> result = new ArrayList<>();
        for( T service : serviceLoaderFinder.load( spiClass, serviceClassLoader ) )
        {
            result.add( service );
        }
        result.sort( Comparator.comparing( Ordered::order ) );
        return result;
    }

    interface ServiceLoaderFinder
    {
        <S> Iterable<S> load( Class<S> spiClass, ClassLoader classLoader );
    }
}
