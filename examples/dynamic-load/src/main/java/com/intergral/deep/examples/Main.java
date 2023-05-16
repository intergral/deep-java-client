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
        System.setProperty( "nv.jar.path", jarPath.toString() );
        Deep.start();
        final Deep instance = Deep.getInstance();
        System.out.println(instance.<IDeep>api().getVersion());
        System.out.println(instance.<IReflection>reflection());

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
        for( ;; )
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
            System.out.println(engineFactory.getNames());
        }

        javax.script.ScriptEngine engine = mgr.getEngineByName( "JavaScript" );
        final javax.script.Bindings bindings = engine.createBindings();
        bindings.put( "obj", hashMap );
        bindings.put( "person", new Person( new Person( "mary" ), "bob" ) );

        System.out.println( engine.eval( "person", bindings ) );
        System.out.println( engine.eval( "person.getParent()", bindings ) );
    }
}
