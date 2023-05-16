package com.intergral.deep.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class BaseTest
{
    protected final Properties systemProps = System.getProperties();


    public String newId()
    {
        return UUID.randomUUID().toString();
    }


    public Map<Character, Integer> makeCharCountMap( final String str )
    {
        final HashMap<Character, Integer> res = new HashMap<Character, Integer>();

        for( int i = 0; i < str.length(); i++ )
        {
            final char c = str.charAt( i );
            final Integer cnt = res.get( c );
            if( cnt == null )
            {
                res.put( c, 0 );
            }
            else
            {
                res.put( c, cnt + 1 );
            }
        }

        return res;
    }
}
