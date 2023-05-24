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

package com.intergral.deep.agent.api.utils;

import java.util.Iterator;

public class ArrayIterator<T> implements Iterator<T>
{
    private final T[] value;
    private int index = 0;

    public ArrayIterator( final T[] value )
    {
        this.value = value;
    }

    @Override
    public boolean hasNext()
    {
        return index < value.length;
    }

    @Override
    public T next()
    {
        final T o = value[index];
        index++;
        return o;
    }
}