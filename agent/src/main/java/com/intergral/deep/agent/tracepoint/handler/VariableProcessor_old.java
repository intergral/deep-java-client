//package com.intergral.deep.agent.tracepoint.handler;
//
//import com.intergral.deep.agent.ReflectionUtils;
//import com.intergral.deep.agent.Utils;
//import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.reflect.Array;
//import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//public class VariableProcessor_old
//{
//    private static final Logger LOGGER = LoggerFactory.getLogger( VariableProcessor_old.class );
//    private static final Set<Class<?>> NO_CHILDREN_TYPES = new HashSet<>();
//
//    static
//    {
//        NO_CHILDREN_TYPES.add( String.class );
//        NO_CHILDREN_TYPES.add( Character.class );
//        NO_CHILDREN_TYPES.add( char.class );
//        NO_CHILDREN_TYPES.add( Number.class );
//        NO_CHILDREN_TYPES.add( Boolean.class );
//        NO_CHILDREN_TYPES.add( boolean.class );
//        NO_CHILDREN_TYPES.add( Long.class );
//        NO_CHILDREN_TYPES.add( long.class );
//        NO_CHILDREN_TYPES.add( Integer.class );
//        NO_CHILDREN_TYPES.add( int.class );
//        NO_CHILDREN_TYPES.add( Short.class );
//        NO_CHILDREN_TYPES.add( short.class );
//        NO_CHILDREN_TYPES.add( Float.class );
//        NO_CHILDREN_TYPES.add( float.class );
//        NO_CHILDREN_TYPES.add( Double.class );
//        NO_CHILDREN_TYPES.add( double.class );
//        NO_CHILDREN_TYPES.add( Byte.class );
//        NO_CHILDREN_TYPES.add( byte.class );
//        NO_CHILDREN_TYPES.add( Class.class );
//    }
//
//    protected SnapshotConfig config;
//
//    public void setConfig( final SnapshotConfig config )
//    {
//        this.config = config;
//    }
//
//    public IProcessResponse process( final SnapshotHandler snapshotHandler,
//                                     final String name,
//                                     final Object value )
//    {
//        final int identityHashCode = System.identityHashCode( value );
//        final Integer integer = snapshotHandler.checkId( identityHashCode );
//        if( integer != null )
//        {
//            final Map<String, Object> varIdentity = new HashMap<>();
//            varIdentity.put( "id", integer );
//            varIdentity.put( "name", name );
//            return new IProcessResponse()
//            {
//                @Override
//                public Map<String, Object> result()
//                {
//                    return varIdentity;
//                }
//
//
//                @Override
//                public boolean processChildren()
//                {
//                    return false;
//                }
//            };
//        }
//
//        final Integer id = snapshotHandler.checkSizeAndNewId( identityHashCode );
//        snapshotHandler.count();
//        final Map<String, Object> varIdentity = new HashMap<>();
//
//        varIdentity.put( "id", id );
//        varIdentity.put( "name", name );
//
//        final Map<String, Object> varValue = new HashMap<>( varIdentity );
//
//        if( value != null )
//        {
//            varValue.put( "type", value.getClass().getName() );
//            if( mapType( value ) || collectionType( value ) || arrayType( value ) )
//            {
//                varValue.put( "value", "Object of type: " + value.getClass().getName() );
//            }
//            else
//            {
//                final Utils.ITrimResult trim = Utils.trim( Utils.valueOf( value ),
//                        VariableProcessor_old.this.config.getMaxStrLength() );
//                varValue.put( "value", trim.value() );
//                if( trim.truncated() )
//                {
//                    putOrAppendFlags( varValue, "truncated" );
//                }
//            }
//        }
//        else
//        {
//            varValue.put( "type", null );
//            varValue.put( "value", null );
//        }
//        varValue.put( "hash", identityHashCode );
//        varValue.put( "children", new HashSet<>() );
//
//        snapshotHandler.updateVarLookup( id, varValue );
//
//        return new IProcessResponse()
//        {
//            @Override
//            public Map<String, Object> result()
//            {
//                return varIdentity;
//            }
//
//
//            @Override
//            public boolean processChildren()
//            {
//                return true;
//            }
//        };
//    }
//
//    private void putOrAppendFlags( final Map<String, Object> varValue, final String... truncated )
//    {
//        final Object flags = varValue.get( "flags" );
//        if( !(flags instanceof List) )
//        {
//            varValue.put( "flags", new ArrayList<>( Arrays.asList( truncated ) ) );
//        }
//        else
//        {
//            //noinspection unchecked
//            varValue.put( "flags", ((List<String>) flags).addAll( Arrays.asList( truncated ) ) );
//        }
//    }
//
//    private boolean mapType( final Object value )
//    {
//        return value instanceof Map;
//    }
//
//    private boolean arrayType( final Object value )
//    {
//        return value != null && value.getClass().isArray();
//    }
//
//    private boolean collectionType( final Object value )
//    {
//        return value instanceof Collection;
//    }
//
//    private boolean noChildrenType( final Object value )
//    {
//        if( value == null )
//        {
//            return true;
//        }
//        return NO_CHILDREN_TYPES.contains( value.getClass() );
//    }
//
//    public Object fieldValue( final Field field, final Object value )
//    {
//        if( !ReflectionUtils.getReflection().setAccessible( value.getClass(), field ) )
//        {
//            return null;
//        }
//
//        final Object fieldValue;
//        if( Modifier.isStatic( field.getModifiers() ) )
//        {
//            try
//            {
//                fieldValue = field.get( null );
//            }
//            catch( IllegalAccessException e )
//            {
//                LOGGER.debug( "Error processing variable: {}", value, e );
//                return null;
//            }
//        }
//        else
//        {
//            try
//            {
//                fieldValue = field.get( value );
//            }
//            catch( IllegalAccessException e )
//            {
//                LOGGER.debug( "Error processing variable: {}", value, e );
//                return null;
//            }
//        }
//        return fieldValue;
//    }
//
//    public Set<Node> processChildNodes( final SnapshotHandler handler,
//                                        final int id,
//                                        final Object value,
//                                        final int depth )
//    {
//        if( value == null || noChildrenType( value ) )
//        {
//            return Collections.emptySet();
//        }
//        final Node.IParent parent = new Node.IParent()
//        {
//            @Override
//            public void addChild( final Map<String, Object> child )
//            {
//                final Map<String, Object> lookup = handler.getVarLookup().get( String.valueOf( id ) );
//                if( lookup != null )
//                {
//                    ((Set) lookup.get( "children" )).add( child );
//                }
//            }
//
//
//            @Override
//            public void flag( final String flag )
//            {
//                putOrAppendFlags( handler.getVarLookup().get( String.valueOf( id ) ), flag );
//            }
//        };
//        if( depth + 1 >= config.getMaxVarDepth() )
//        {
//            putOrAppendFlags( handler.getVarLookup().get( String.valueOf( id ) ), "depth" );
//            return Collections.emptySet();
//        }
//        if( arrayType( value ) )
//        {
//            return processArrayNodes( parent, handler, value );
//        }
//        if( collectionType( value ) )
//        {
//            return processArrayNodes( parent, handler, ((Collection<?>) value).toArray() );
//        }
//        if( mapType( value ) )
//        {
//            return processMapNodes( parent, handler, (Map) value );
//        }
//        return processFieldNodes( parent, value );
//    }
//
//    public Set<Node> processFieldNodes( final Node.IParent parent,
//                                        final Object value )
//    {
//        // TODO: 23.06.21 should we walk the super classes to collect all the fields?
//        final Field[] fields = value.getClass().getFields();
//        final Field[] declaredFields = value.getClass().getDeclaredFields();
//        final HashSet<Node> nodes = new HashSet<>();
//        for( final Field field : fields )
//        {
//            final Object fieldValue = fieldValue( field, value );
//            final Node node = new Node( new Node.IVarAccessor()
//            {
//                @Override
//                public String name()
//                {
//                    return field.getName();
//                }
//
//
//                @Override
//                public Object value()
//                {
//                    return fieldValue;
//                }
//            } );
//            node.setParent( parent );
//            nodes.add( node );
//        }
//        for( final Field field : declaredFields )
//        {
//            final Object fieldValue = fieldValue( field, value );
//            final Node node = new Node( new Node.IVarAccessor()
//            {
//                @Override
//                public String name()
//                {
//                    return field.getName();
//                }
//
//
//                @Override
//                public Object value()
//                {
//                    return fieldValue;
//                }
//            } );
//            node.setParent( parent );
//            nodes.add( node );
//        }
//        return nodes;
//    }
//
//    protected Set<Node> processMapNodes( final Node.IParent parent,
//                                         final SnapshotHandler handler,
//                                         final Map<Object, Object> value )
//    {
//        return this.processMapNodesWithLimit( parent, handler, value, config.getMaxCollectionSize() );
//    }
//
//    protected Set<Node> processMapNodesWithLimit( final Node.IParent parent,
//                                                  final SnapshotHandler handler,
//                                                  final Map<Object, Object> value,
//                                                  final int limit )
//    {
//        final Set<Node> nodes = new HashSet<>();
//        final List<Map.Entry<Object, Object>> entries = new ArrayList<Map.Entry<Object, Object>>( value.entrySet() );
//
//        for( int i = 0, keyListSize = entries.size(); i < keyListSize; i++ )
//        {
//            final Map.Entry<Object, Object> entry = entries.get( i );
//            final Object key = entry.getKey();
//            final Object val = entry.getValue();
//            final NVMapEntry nvMapEntry = new NVMapEntry( key, val );
//            final int finalI = i;
//            final Node node;
//            // to improve the UI rendering - if the key is a no child type then just the value directly
//            if( NO_CHILDREN_TYPES.contains( key.getClass() ) )
//            {
//                node = new Node( new Node.IVarAccessor()
//                {
//                    @Override
//                    public String name()
//                    {
//                        return Utils.trim( String.valueOf( key ), VariableProcessor_old.this.config.getMaxStrLength() )
//                                .value();
//                    }
//
//
//                    @Override
//                    public Object value()
//                    {
//                        return val;
//                    }
//                } );
//            }
//            else
//            {
//                node = new Node( new Node.IVarAccessor()
//                {
//                    @Override
//                    public String name()
//                    {
//                        return String.valueOf( finalI );
//                    }
//
//
//                    @Override
//                    public Object value()
//                    {
//                        return nvMapEntry;
//                    }
//                } );
//            }
//            node.setParent( parent );
//            nodes.add( node );
//            if( limit != -1 && i >= limit )
//            {
//                parent.flag( "list_truncated" );
//                break;
//            }
//        }
//
//        return nodes;
//    }
//
//    private Set<Node> processArrayNodes( final Node.IParent parent,
//                                         final SnapshotHandler handler,
//                                         final Object value )
//    {
//        final Set<Node> nodes = new HashSet<>();
//        final int len = Array.getLength( value );
//
//        for( int i = 0; i < len; i++ )
//        {
//            final Object val = Array.get( value, i );
//
//            final int finalI = i;
//            final Node node = new Node( new Node.IVarAccessor()
//            {
//                @Override
//                public String name()
//                {
//                    return String.valueOf( finalI );
//                }
//
//
//                @Override
//                public Object value()
//                {
//                    return val;
//                }
//            } );
//            node.setParent( parent );
//            nodes.add( node );
//            if( i >= handler.config.getMaxCollectionSize() )
//            {
//                parent.flag( "list_truncated" );
//                break;
//            }
//        }
//        return nodes;
//    }
//
//
//    public interface IProcessResponse
//    {
//        Map<String, Object> result();
//
//        boolean processChildren();
//    }
//
//    public static class NVMapEntry
//    {
//        public Object key;
//        public Object value;
//
//
//        public NVMapEntry( final Object key, final Object value )
//        {
//            this.key = key;
//            this.value = value;
//        }
//    }
//}
