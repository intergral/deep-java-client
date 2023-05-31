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

package com.intergral.deep.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ResettableCountDownLatch
{
    private final int initialCount;
    private volatile CountDownLatch latch;


    public ResettableCountDownLatch( int count )
    {
        initialCount = count;
        latch = new CountDownLatch( count );
    }


    public void reset()
    {
        latch = new CountDownLatch( initialCount );
    }


    public void reset( int count )
    {
        latch = new CountDownLatch( count );
    }


    public long getCount()
    {
        return latch.getCount();
    }


    public void countDown()
    {
        latch.countDown();
    }


    public void await() throws InterruptedException
    {
        latch.await();
    }


    public boolean await( long timeout, TimeUnit unit ) throws InterruptedException
    {
        return latch.await( timeout, unit );
    }
}
