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

package com.intergral.deep.agent.tracepoint.handler.bfs;

import com.intergral.deep.agent.types.snapshot.VariableID;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A node is a value to process in the BFS. It also links children to parents to ensure hierarchy in variables.
 */
public class Node {

  final NodeValue value;
  final Set<Node> children;
  final IParent parent;
  int depth = 0;


  /**
   * Create a new node for the BFS.
   *
   * @param value  the value to wrap
   * @param parent the parent of this node
   */
  public Node(final NodeValue value, final IParent parent) {
    this(value, new HashSet<>(), parent);
  }


  /**
   * Create a new node for the BFS.
   *
   * @param value    the value to wrap
   * @param children the children of this node
   * @param parent   the parent of this node
   */
  public Node(final NodeValue value, final Set<Node> children, final IParent parent) {
    this.value = value;
    this.parent = parent;
    this.children = children;
  }

  /**
   * Start the breadth first search of the nodes.
   *
   * @param root     the root node to start from
   * @param consumer the consumer to use to collect more nodes.
   */
  public static void breadthFirstSearch(final Node root, final IConsumer consumer) {
    final List<Node> queue = new LinkedList<>();

    queue.add(root);

    while (!queue.isEmpty()) {
      final Node n = queue.remove(0);
      final boolean canContinue = consumer.processNode(n);

      if (canContinue) {
        queue.addAll(n.children);
      } else {
        return;
      }
    }
  }

  /**
   * Add child nodes.
   *
   * @param children the children to add
   */
  public void addChildren(final Set<Node> children) {
    for (Node child : children) {
      child.depth = this.depth + 1;
      this.children.add(child);
    }
  }


  public NodeValue getValue() {
    return value;
  }


  public IParent getParent() {
    return parent;
  }

  public int depth() {
    return depth;
  }


  /**
   * The parent of a processed node.
   */
  public interface IParent {

    void addChild(final VariableID child);

    default boolean isCollection() {
      return false;
    }
  }


  /**
   * The consumer of the nodes when running a BFS.
   */
  public interface IConsumer {

    boolean processNode(Node node);
  }


  /**
   * This type wraps an Object that we are to process. They simply acts as a reference to the read data during the Breadth First search.
   */
  public static class NodeValue {

    private final String name;
    private final Object value;
    private final String originalName;
    private final Set<String> modifiers;

    public NodeValue(final String key, final Object value) {
      this(key, value, null, Collections.emptySet());
    }

    /**
     * Create a new node value.
     *
     * @param name         the name of the value
     * @param value        the value to wrap
     * @param originalName the original name of the value
     * @param modifiers    the value modifiers
     */
    public NodeValue(final String name, final Object value, final String originalName,
        final Set<String> modifiers) {
      this.name = name;
      this.value = value;
      this.originalName = originalName;
      this.modifiers = modifiers;
    }

    public String getName() {
      return name;
    }

    public Object getValue() {
      return value;
    }

    public Set<String> getModifiers() {
      return this.modifiers;
    }

    public String getOriginalName() {
      return this.originalName;
    }
  }
}
