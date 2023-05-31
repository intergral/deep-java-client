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


import com.intergral.deep.Deep;
import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.reflection.IReflection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Main
{
    public static void main( String[] args ) throws Throwable
    {
        new Main().name();
        Path jarPath = Paths.get( Main.class.getResource( "/" ).toURI() )
                .getParent()
                .getParent()
                .getParent()
                .getParent()
                .resolve( "agent/target/deep-1.0-SNAPSHOT.jar" );
        System.setProperty( "deep.jar.path", jarPath.toString() );
        Deep.config()
                .setValue( "logging.level", "FINE" )
                .setValue( "service.url", "localhost:43315" )
                .setValue( "service.secure", false )
                .start();

        final Deep instance = Deep.getInstance();
        System.out.println( instance.<IDeep>api().getVersion() );
        System.out.println( instance.<IReflection>reflection() );

//        try
//        {
//            System.out.println( NerdVisionAPI.getInstance().version().version() );
//        }
//        catch( Exception e )
//        {
//            e.printStackTrace();
//        }
//        final NerdVision nv = NerdVision.getInstance();
//        nv.await();
        //        nv.configure( new HashMap<String, String>()
        //        {{
        //            put( "logging.level", "DEBUG" );
        //        }} );
//        NerdVision.start(
//                "nv-2AWmgeFph8M8ZI0HUW5K",
//                "java-poc", new HashMap<String, String>()
//                {{
//                    put( "key", "val" );
//                }} );
//        System.out.println( NerdVisionAPI.getInstance().version().version() );
//        //        nv.addBreakpoint( Main.class, 37 );
//        NerdVisionAPI.getInstance().updateBreakpoints();

//        int cnt = 0;
//        while( cnt < 1000 )
//        {
//            new Main().something( "hello me" );
//            Thread.sleep( 1000 );
//            cnt++;
//        }

        final SimpleTest ts = new SimpleTest( "This is a test", 2 );
        for( ; ; )
        {
            try
            {
                ts.message( ts.newId() );
            }
            catch( Exception e )
            {
                e.printStackTrace();
//                NerdVisionAPI.getInstance().captureException( e );
            }

            Thread.sleep( 1000 );
        }

    }

    public void something( final String name )
    {
        System.out.println( "something" );
        System.out.println( name );
        System.out.println( "something" );
    }

    public void name() throws Exception
    {
        final HashMap<String, String> hashMap = new HashMap<String, String>()
        {{
            put( "name", "ben" );
        }};
        javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
        final List<javax.script.ScriptEngineFactory> engineFactories = mgr.getEngineFactories();

        for( javax.script.ScriptEngineFactory engineFactory : engineFactories )
        {
            System.out.println( engineFactory.getNames() );
        }

        javax.script.ScriptEngine engine = mgr.getEngineByName( "JavaScript" );
        final javax.script.Bindings bindings = engine.createBindings();
        bindings.put( "obj", hashMap );
        bindings.put( "person", new Person( new Person( "mary" ), "bob" ) );

        System.out.println( engine.eval( "person", bindings ) );
        System.out.println( engine.eval( "person.getParent()", bindings ) );
    }

    public static class Person
    {
        private final Person parent;
        private final String name;


        public Person( final String name )
        {
            this( null, name );
        }


        public Person( final Person parent, final String name )
        {
            this.parent = parent;
            this.name = name;
        }


        public Person getParent()
        {
            return parent;
        }


        public String getName()
        {
            return name;
        }
    }
}
