package com.intergral.deep.agent.tracepoint.handler.bfs;

import com.intergral.deep.agent.types.snapshot.VariableID;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Node
{
    final NodeValue value;
    final Set<Node> children;
    final IParent parent;
    int depth = 0;


    public Node( final NodeValue value, final IParent parent )
    {
        this( value, new HashSet<>(), parent );
    }


    public Node( final NodeValue value, final Set<Node> children, final IParent parent )
    {
        this.value = value;
        this.parent = parent;
        this.children = children;
    }

    public static void breadthFirstSearch( final Node root, final IConsumer consumer )
    {
        final List<Node> queue = new LinkedList<>();

        queue.add( root );

        while( !queue.isEmpty() )
        {
            final Node n = queue.remove( 0 );
            final boolean canContinue = consumer.processNode( n );

            if( canContinue )
            {
                queue.addAll( n.children );
            }
            else
            {
                return;
            }
        }
    }

    public void addChildren( final Set<Node> children )
    {
        for( Node child : children )
        {
            child.depth = this.depth + 1;
            this.children.add( child );
        }
    }


    public NodeValue getValue()
    {
        return value;
    }


    public IParent getParent()
    {
        return parent;
    }

    public int depth()
    {
        return depth;
    }


    public interface IParent
    {
        void addChild( final VariableID child );
    }


    public interface IConsumer
    {
        boolean processNode( Node node );
    }


    public static class NodeValue
    {
        private final String key;
        private final Object value;
        private final String originalName;
        private final Set<String> modifiers;

        public NodeValue( final String key, final Object value )
        {
            this( key, value, null, Collections.emptySet() );
        }

        public NodeValue( final String key, final Object value, final String originalName, final Set<String> modifiers )
        {
            this.key = key;
            this.value = value;
            this.originalName = originalName;
            this.modifiers = modifiers;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public Set<String> getModifiers()
        {
            return this.modifiers;
        }

        public String getOriginalName()
        {
            return this.originalName;
        }
    }
}
