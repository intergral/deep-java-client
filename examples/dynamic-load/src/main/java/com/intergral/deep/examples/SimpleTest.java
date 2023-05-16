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
            final Integer i = newInfo.get(c);

            Integer curr = charCounter.get(c);
            if (curr == null)
            {
                charCounter.put(c, i);
            }
            else
            {
                charCounter.put(c, curr + i);
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
