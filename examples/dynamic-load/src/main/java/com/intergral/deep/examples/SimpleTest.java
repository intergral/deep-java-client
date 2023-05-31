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

package com.intergral.deep.examples;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class SimpleTest extends BaseTest
{
    public static Date NICE_START_DATE = new Date();

    private final long startedAt = System.currentTimeMillis();
    private final String testName;
    private final int maxExecutions;
    private int cnt = 0;
    private Map<Character, Integer> charCounter = new TreeMap<Character, Integer>();


    public SimpleTest( final String testName, final int maxExecutions )
    {
        this.testName = testName;
        this.maxExecutions = maxExecutions;
    }


    void message( final String uuid ) throws Exception
    {
        System.out.println( cnt + ":" + uuid );
        cnt += 1;

        checkEnd( cnt, maxExecutions );

        final Map<Character, Integer> info = makeCharCountMap( uuid );
        merge( charCounter, info );
        if( (cnt % 30) == 0 )
        {
            dump();
        }
    }


    void merge( final Map<Character, Integer> charCounter, final Map<Character, Integer> newInfo )
    {
        for( final Character c : newInfo.keySet() )
        {
            final Integer i = newInfo.get( c );

            Integer curr = charCounter.get( c );
            if( curr == null )
            {
                charCounter.put( c, i );
            }
            else
            {
                charCounter.put( c, curr + i );
            }
        }
    }


    void dump()
    {
        System.out.println( charCounter );
        charCounter = new HashMap<Character, Integer>();
    }


    static void checkEnd( final int val, final int max ) throws Exception
    {
        if( val > max )
        {
            throw new Exception( "Hit max executions " + val + " " + max );
        }
    }


    @Override
    public String toString()
    {
        return getClass().getName() + ":" + testName + ":" + startedAt + "#" + System.identityHashCode( this );
    }
}
