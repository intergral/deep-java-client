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

package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.ReflectionUtils;
import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.utils.ArrayObjectIterator;
import com.intergral.deep.agent.api.utils.CompoundIterator;
import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class VariableProcessor
{
    private static final Set<Class<?>> NO_CHILDREN_TYPES = new HashSet<>();

    static
    {
        NO_CHILDREN_TYPES.add( String.class );
        NO_CHILDREN_TYPES.add( Character.class );
        NO_CHILDREN_TYPES.add( char.class );
        NO_CHILDREN_TYPES.add( Number.class );
        NO_CHILDREN_TYPES.add( Boolean.class );
        NO_CHILDREN_TYPES.add( boolean.class );
        NO_CHILDREN_TYPES.add( Long.class );
        NO_CHILDREN_TYPES.add( long.class );
        NO_CHILDREN_TYPES.add( Integer.class );
        NO_CHILDREN_TYPES.add( int.class );
        NO_CHILDREN_TYPES.add( Short.class );
        NO_CHILDREN_TYPES.add( short.class );
        NO_CHILDREN_TYPES.add( Float.class );
        NO_CHILDREN_TYPES.add( float.class );
        NO_CHILDREN_TYPES.add( Double.class );
        NO_CHILDREN_TYPES.add( double.class );
        NO_CHILDREN_TYPES.add( Byte.class );
        NO_CHILDREN_TYPES.add( byte.class );
        NO_CHILDREN_TYPES.add( Class.class );
        NO_CHILDREN_TYPES.add( Iterator.class );
    }

    protected final FrameConfig frameConfig = new FrameConfig();
    protected Map<String, Variable> varLookup = new HashMap<>();

    protected Set<Node> processChildNodes( final VariableID variableId, final Object value, final int depth )
    {
        // if the type is a type we do not want children from - return empty
        final Class<?> varType = value.getClass();
        if( NO_CHILDREN_TYPES.contains( varType ) )
        {
            return Collections.emptySet();
        }

        // if the depth is more than we are configured - return empty
        if( !checkDepth( depth ) )
        {
            return Collections.emptySet();
        }

        final Node.IParent parent = ( node ) -> this.appendChild( variableId.getId(), node );

        return findChildrenForParent( parent, value );
    }

    private Set<Node> findChildrenForParent( final Node.IParent parent, final Object value )
    {
        if( arrayType( value ) )
        {
            return processNamedIterable( parent, new NamedIterable<>( new ArrayObjectIterator( value ) ) );
        }
        if( collectionType( value ) )
        {
            return processNamedIterable( parent, new NamedIterable<>( ((Collection<Object>) value).iterator() ) );
        }
        if( mapType( value ) )
        {
            return processNamedIterable( parent,
                    new NamedMapIterator( ((Map<Object, Object>) value).entrySet().iterator() ) );
        }
        return processNamedIterable( parent, new NamesFieldIterator( value, new FieldIterator( value.getClass() ) ) );
    }


    private Set<Node> processNamedIterable( final Node.IParent parent, final NamedIterable<?> objectNamedIterable )
    {
        final HashSet<Node> nodes = new HashSet<>();

        while( objectNamedIterable.hasNext() )
        {
            final NamedIterable.INamedItem next = objectNamedIterable.next();

            final Node node = new Node( new Node.NodeValue( next.name(),
                    next.item(),
                    next.originalName(),
                    next.modifiers() ), parent );
            nodes.add( node );

            if( nodes.size() > this.frameConfig.maxCollectionSize() )
            {
                break;
            }
        }

        return nodes;
    }

    private boolean mapType( final Object value )
    {
        return value instanceof Map;
    }

    private boolean arrayType( final Object value )
    {
        return value != null && value.getClass().isArray();
    }

    private boolean collectionType( final Object value )
    {
        return value instanceof Collection;
    }


    protected VariableResponse processVariable( final Node.NodeValue value )
    {
        final Object objValue = value.getValue();
        // get the variable hash id
        final String identityCode = String.valueOf( System.identityHashCode( objValue ) );

        //check the collector cache for this id
        final String cacheId = checkId( identityCode );
        // if we have a cache_id, then this variable is already been processed, so we just return
        // a variable id and do not process children. This prevents us from processing the same value over and over. We
        // also do not count this towards the max_vars, so we can increase the data we send.

        if( cacheId != null )
        {
            return new VariableResponse( new VariableID( cacheId,
                    value.getKey(),
                    value.getModifiers(),
                    value.getOriginalName() ), false );
        }

        // if we do not have a cache_id - then create one
        final String varId = newVarId( identityCode );
        final VariableID variableId = new VariableID( varId,
                value.getKey(),
                value.getModifiers(),
                value.getOriginalName() );

        final String varType;
        if( objValue == null )
        {
            varType = "null";
        }
        else
        {
            varType = objValue.getClass().getName();
        }

        final Utils.ITrimResult iTrimResult = Utils.trim( this.valueToString( objValue ),
                this.frameConfig.maxStringLength() );

        final Variable variable = new Variable( varType, iTrimResult.value(), identityCode, iTrimResult.truncated() );

        appendVariable( varId, variable );

        return new VariableResponse( variableId, true );
    }

    private String valueToString( final Object value )
    {
        if( value instanceof Collection )
        {
            return String.format( "Collection of size: %s", ((Collection<?>) value).size() );
        }
        else if( value instanceof Iterator )
        {
            return String.format( "Iterator of type: %s", value.getClass().getSimpleName() );
        }
        else if( value instanceof Map )
        {
            return String.format( "Map of size: %s", ((Map<?, ?>) value).size() );
        }
        return Utils.valueOf( value );
    }

    protected boolean checkDepth( final int depth )
    {
        return depth + 1 < this.frameConfig.maxDepth();
    }

    protected abstract void appendChild( final String parentId, final VariableID variableId );

    protected abstract void appendVariable( final String varId, final Variable variable );

    protected abstract String checkId( final String identity );

    protected abstract String newVarId( final String identity );

    protected static class VariableResponse
    {

        private final VariableID variableId;
        private final boolean processChildren;

        public VariableResponse( final VariableID variableId, final boolean processChildren )
        {
            this.variableId = variableId;
            this.processChildren = processChildren;
        }

        public VariableID getVariableId()
        {
            return variableId;
        }

        public boolean processChildren()
        {
            return processChildren;
        }
    }

    private static class NamedIterable<T>
            implements Iterator<NamedIterable.INamedItem>
    {
        protected final Iterator<T> iterator;
        private int index = 0;

        public NamedIterable( final Iterator<T> iterator )
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return this.iterator.hasNext();
        }

        @Override
        public NamedIterable.INamedItem next()
        {
            final String name = String.valueOf( index++ );
            final Object next = this.iterator.next();
            return new INamedItem()
            {
                @Override
                public String name()
                {
                    return name;
                }

                @Override
                public Object item()
                {
                    return next;
                }

                @Override
                public String originalName()
                {
                    return null;
                }

                @Override
                public Set<String> modifiers()
                {
                    return Collections.emptySet();
                }
            };
        }

        public interface INamedItem
        {
            String name();

            Object item();

            String originalName();

            Set<String> modifiers();
        }
    }

    private static class NamedMapIterator extends NamedIterable<Map.Entry<Object, Object>>
    {

        public NamedMapIterator( final Iterator<Map.Entry<Object, Object>> iterator )
        {
            super( iterator );
        }

        @Override
        public INamedItem next()
        {
            final Map.Entry<?, ?> next = this.iterator.next();
            return new INamedItem()
            {
                @Override
                public String name()
                {
                    return String.valueOf( next.getKey() );
                }

                @Override
                public Object item()
                {
                    return next.getValue();
                }

                @Override
                public String originalName()
                {
                    return null;
                }

                @Override
                public Set<String> modifiers()
                {
                    return Collections.emptySet();
                }
            };
        }
    }

    private static class NamesFieldIterator extends NamedIterable<Field>
    {

        private final Object target;

        public NamesFieldIterator( final Object target, final Iterator<Field> iterator )
        {
            super( iterator );
            this.target = target;
        }

        @Override
        public INamedItem next()
        {
            final Field next = this.iterator.next();
            return new INamedItem()
            {
                @Override
                public String name()
                {
                    return next.getName();
                }

                @Override
                public Object item()
                {
                    return ReflectionUtils.callField( target, next );
                }

                @Override
                public String originalName()
                {
                    return null;
                }

                @Override
                public Set<String> modifiers()
                {
                    return ReflectionUtils.getModifiers( next );
                }
            };
        }
    }

    private static class FieldIterator implements Iterator<Field>
    {
        private final Class<?> clazz;
        private final Iterator<Field> iterator;

        public FieldIterator( final Class<?> clazz )
        {
            this.clazz = clazz;
            if( this.clazz.getSuperclass() == null || this.clazz.getSuperclass() == Object.class )
            {
                this.iterator = ReflectionUtils.getFieldIterator( clazz );
            }
            else
            {
                this.iterator = new CompoundIterator<>( ReflectionUtils.getFieldIterator( clazz ),
                        new FieldIterator( this.clazz.getSuperclass() ) );
            }
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public Field next()
        {
            return iterator.next();
        }
    }
}
