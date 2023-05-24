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
import java.util.function.Predicate;

public class FilteredIterator<T> implements Iterator<T>
{
    private final Iterator<T> source;
    private final Predicate<T> predicate;
    private T next;

    public FilteredIterator( final Iterator<T> source, final Predicate<T> predicate )
    {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public boolean hasNext()
    {
        final boolean hasNext = this.source.hasNext();
        if( !hasNext )
        {
            return hasNext;
        }
        next = this.source.next();
        while( !predicate.test( next ) )
        {
            next = this.source.next();
        }
        return next != null;
    }

    @Override
    public T next()
    {
        return next;
    }
}
