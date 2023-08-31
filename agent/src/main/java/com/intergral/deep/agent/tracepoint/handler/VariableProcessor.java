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
import com.intergral.deep.agent.tracepoint.handler.bfs.Node.IParent;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This type deals with processing the variables. Dealing with:
 * <ul>
 *  <li>type definition - how the type of the variable is captured</li>
 *  <li>string values - how the string representation of the variable value is captured</li>
 *  <li>child variables - how many child variables are processed</li>
 *  <li>variable limits - total limits on how many variables are processed</li>
 *  <li>depth limits - how deep will process down the reference chain</li>
 *  <li>variable deduplication - ensuring we do not process the same variable multiple times</li>
 *  <li>variable referencing - using variable ids to reference already processed variables</li>
 * </ul>
 * <p>
 * While processing a variable or frame, we will process using a Breadth first approach. These means given the tree:
 * <pre>
 *   1 -&gt; 1.1
 *        1.2
 *        1.3 -&gt; 1.3.1
 *   2 -&gt; 2.1
 *   3 -&gt; 3.1 -&gt; 3.1.1
 * </pre>
 * We will attempt to gather the variables in the order:
 * <ul>
 *   <li>1</li>
 *   <li>2</li>
 *   <li>3</li>
 *   <li>1.1</li>
 *   <li>1.2</li>
 *   <li>1.3</li>
 *   <li>2.1</li>
 *   <li>3.1</li>
 *   <li>1.3.1</li>
 *   <li>3.1.1</li>
 * </ul>
 * This ensures that we capture the variables closer to the tracepoint before we go deeper.
 */
public abstract class VariableProcessor {

  /**
   * These are teh class names of types that we do not want to collect child variables from. These are mostly primitive or object types that
   * the string value will provide the full value of, or where child values do not add value.
   */
  private static final Set<Class<?>> NO_CHILDREN_TYPES = new HashSet<>();

  static {
    NO_CHILDREN_TYPES.add(String.class);
    NO_CHILDREN_TYPES.add(Character.class);
    NO_CHILDREN_TYPES.add(char.class);
    NO_CHILDREN_TYPES.add(Number.class);
    NO_CHILDREN_TYPES.add(Boolean.class);
    NO_CHILDREN_TYPES.add(boolean.class);
    NO_CHILDREN_TYPES.add(Long.class);
    NO_CHILDREN_TYPES.add(long.class);
    NO_CHILDREN_TYPES.add(Integer.class);
    NO_CHILDREN_TYPES.add(int.class);
    NO_CHILDREN_TYPES.add(Short.class);
    NO_CHILDREN_TYPES.add(short.class);
    NO_CHILDREN_TYPES.add(Float.class);
    NO_CHILDREN_TYPES.add(float.class);
    NO_CHILDREN_TYPES.add(Double.class);
    NO_CHILDREN_TYPES.add(double.class);
    NO_CHILDREN_TYPES.add(Byte.class);
    NO_CHILDREN_TYPES.add(byte.class);
    NO_CHILDREN_TYPES.add(Class.class);
    NO_CHILDREN_TYPES.add(Iterator.class);
  }

  /**
   * Some config values from the triggered tracepoints affect all tracepoints at the point of collection. This {@link FrameConfig}
   * calculates the most encompassing config for all triggered tracepoints.
   */
  protected final FrameConfig frameConfig = new FrameConfig();
  /**
   * This is the cache we use while building this lookup, this cache essentially maps {@link System#identityHashCode(Object)} to an internal
   * id (monotonically incrementing id) used to deduplicate the variables. Essentially allows us to map the same object via different
   * references, meaning if we are processing a type that references its self, or has multiple paths to the same object we will be process
   * it again.
   */
  private final Map<String, String> varCache = new HashMap<>();
  /**
   * This is the lookup that we are building from this processor. It will contain the deduplicated variables by reference.
   */
  Map<String, Variable> varLookup = new HashMap<>();

  //todo i do not like this approach to getting an empty var look up for watches
  protected Map<String, Variable> closeLookup() {
    final Map<String, Variable> lookup = varLookup;
    varLookup = new HashMap<>();
    return lookup;
  }

  public void configureSelf(final Iterable<TracePointConfig> configs) {
    for (TracePointConfig tracePointConfig : configs) {
      this.frameConfig.process(tracePointConfig);
    }
    this.frameConfig.close();
  }

  protected Set<Node> processChildNodes(final VariableID variableId, final Object value,
      final int depth) {
    // if the type is a type we do not want children from - return empty
    final Class<?> varType = value.getClass();
    if (NO_CHILDREN_TYPES.contains(varType)) {
      return Collections.emptySet();
    }

    // if the depth is more than we are configured - return empty
    if (!checkDepth(depth)) {
      return Collections.emptySet();
    }

    final Node.IParent parent = new IParent() {
      @Override
      public void addChild(final VariableID child) {
        VariableProcessor.this.appendChild(variableId.getId(), child);
      }

      @Override
      public boolean isCollection() {
        return VariableProcessor.this.collectionType(value) || VariableProcessor.this.mapType(value) || VariableProcessor.this.arrayType(
            value);
      }
    };

    return findChildrenForParent(parent, value);
  }

  private Set<Node> findChildrenForParent(final Node.IParent parent, final Object value) {
    if (arrayType(value)) {
      return processNamedIterable(parent, new NamedIterable<>(new ArrayObjectIterator(value)));
    }
    if (collectionType(value)) {
      //noinspection unchecked
      return processNamedIterable(parent,
          new NamedIterable<>(((Collection<Object>) value).iterator()));
    }
    if (mapType(value)) {
      //noinspection unchecked
      return processNamedIterable(parent,
          new NamedMapIterator(((Map<Object, Object>) value).entrySet().iterator()));
    }
    return processNamedIterable(parent,
        new NamedFieldIterator(value, new FieldIterator(value.getClass())));
  }


  /**
   * To allow for a common way to process variables from the multiple ways we can gather variables. We introduce the {@link NamedIterable},
   * which allows as to iterate maps, arrays and class fields.
   * <p>
   * This method lets us convert the iterable of names items into {@link Node} for our Breadth First search algorithm.
   *
   * @param parent   the parent node these values belong to
   * @param iterable the iterable to process
   * @return the new set of nodes to process
   */
  private Set<Node> processNamedIterable(final Node.IParent parent,
      final NamedIterable<?> iterable) {
    final HashSet<Node> nodes = new HashSet<>();

    while (iterable.hasNext()) {
      final NamedIterable.INamedItem next = iterable.next();

      final Node node = new Node(new Node.NodeValue(next.name(),
          next.item(),
          next.originalName(),
          next.modifiers()), parent);
      nodes.add(node);

      if (parent.isCollection() && nodes.size() > this.frameConfig.maxCollectionSize()) {
        break;
      }
    }

    return nodes;
  }

  /**
   * Is the passed value a type of map.
   *
   * @param value the object to check
   * @return {@code true} if the object is a {@link Map} type, else {@code false}
   */
  private boolean mapType(final Object value) {
    return value instanceof Map;
  }

  /**
   * Is the object a type of array
   *
   * @param value the object to check
   * @return {@code true} if the object is a form of array, else {@code false}
   * @see Class#isArray()
   */
  private boolean arrayType(final Object value) {
    return value != null && value.getClass().isArray();
  }

  /**
   * Is the object a type of collection, not including array
   *
   * @param value the object to check
   * @return {@code true} if the object is a {@link Collection}, else {@code false}.
   */
  private boolean collectionType(final Object value) {
    return value instanceof Collection;
  }


  /**
   * Process the given node into a {@link VariableResponse}.
   * <p>
   * If the provided value has already been processed, ie the {@link System#identityHashCode(Object)} of the object is already in the
   * {@link #varCache} then we do not process the variable, and simply return a pointer to the reference.
   * <p>
   * If the value has not already been process then we must gather the type, value, children etc. Then process the data into the
   * {@link #varLookup}.
   *
   * @param value the node value to process
   * @return the {@link VariableResponse}
   */
  protected VariableResponse processVariable(final Node.NodeValue value) {
    final Object objValue = value.getValue();
    // get the variable hash id
    final String identityCode = String.valueOf(System.identityHashCode(objValue));

    //check the collector cache for this id
    final String cacheId = checkId(identityCode);
    // if we have a cache_id, then this variable is already been processed, so we just return
    // a variable id and do not process children. This prevents us from processing the same
    // value over and over. We also do not count this towards the max_vars, so we can increase
    // the data we send.

    if (cacheId != null) {
      return new VariableResponse(new VariableID(cacheId,
          value.getKey(),
          value.getModifiers(),
          value.getOriginalName()), false);
    }

    // if we do not have a cache_id - then create one
    final String varId = newVarId(identityCode);
    final VariableID variableId = new VariableID(varId,
        value.getKey(),
        value.getModifiers(),
        value.getOriginalName());

    final String varType;
    if (objValue == null) {
      varType = "null";
    } else {
      varType = objValue.getClass().getName();
    }

    final Utils.ITrimResult iTrimResult = Utils.truncate(this.valueToString(objValue),
        this.frameConfig.maxStringLength());

    final Variable variable = new Variable(varType, iTrimResult.value(), identityCode,
        iTrimResult.truncated());

    appendVariable(varId, variable);

    return new VariableResponse(variableId, true);
  }

  /**
   * Create a string representation of the value.
   * <p>
   * For most objects this is simply the result of {@link Object#toString()}, however for {@link Collection}, {@link Map}, {@link Iterator}
   * and arrays, we create a simplified value as we collect the collection items as children.
   *
   * @param value the value to stringify
   * @return a {@link String} that represents the value
   */
  private String valueToString(final Object value) {
    if (collectionType(value)) {
      return String.format("%s of size: %s", InstUtils.shortClassName(value.getClass().getName()), ((Collection<?>) value).size());
    } else if (value instanceof Iterator) {
      return String.format("Iterator of type: %s", InstUtils.shortClassName(value.getClass().getName()));
    } else if (mapType(value)) {
      return String.format("%s of size: %s", InstUtils.shortClassName(value.getClass().getName()), ((Map<?, ?>) value).size());
    } else if (arrayType(value)) {
      return String.format("Array of length: %s", Array.getLength(value));
    }
    // we must use utils to create the string as it is possible for toString to fail
    return Utils.valueOf(value);
  }

  protected void appendChild(final String parentId, final VariableID variableId) {
    final Variable variable = this.varLookup.get(parentId);
    variable.addChild(variableId);
  }

  protected void appendVariable(final String varId, final Variable variable) {
    this.varLookup.put(varId, variable);
  }

  protected boolean checkDepth(final int depth) {
    return depth + 1 < this.frameConfig.maxDepth();
  }

  protected boolean checkVarCount() {
    return varCache.size() <= this.frameConfig.maxVariables();
  }

  protected String checkId(final String identity) {
    return this.varCache.get(identity);
  }

  protected String newVarId(final String identity) {
    final int size = this.varCache.size();
    final String newId = String.valueOf(size + 1);
    this.varCache.put(identity, newId);
    return newId;
  }

  /**
   * This type is essentially a way to return the {@link VariableID} and to indicate if we need to process the children of this variable.
   */
  protected static class VariableResponse {

    private final VariableID variableId;
    private final boolean processChildren;

    public VariableResponse(final VariableID variableId, final boolean processChildren) {
      this.variableId = variableId;
      this.processChildren = processChildren;
    }

    public VariableID getVariableId() {
      return variableId;
    }

    public boolean processChildren() {
      return processChildren;
    }
  }

  /**
   * This is the basic named iterator that wraps iterator items with a name, e.g. the field name, collection index, or map key
   *
   * @param <T> the type of value we are iterating
   */
  private static class NamedIterable<T>
      implements Iterator<NamedIterable.INamedItem> {

    protected final Iterator<T> iterator;
    private int index = 0;

    public NamedIterable(final Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return this.iterator.hasNext();
    }

    @Override
    public NamedIterable.INamedItem next() {
      final String name = String.valueOf(index++);
      final Object next = this.iterator.next();
      return new INamedItem() {
        @Override
        public String name() {
          return name;
        }

        @Override
        public Object item() {
          return next;
        }

        @Override
        public String originalName() {
          return null;
        }

        @Override
        public Set<String> modifiers() {
          return Collections.emptySet();
        }
      };
    }

    /**
     * The interface of an item named by the named iterator
     */
    public interface INamedItem {

      String name();

      Object item();

      String originalName();

      Set<String> modifiers();
    }
  }

  /**
   * Use the named iterator to name map values with the map key
   */
  private static class NamedMapIterator extends NamedIterable<Map.Entry<Object, Object>> {

    public NamedMapIterator(final Iterator<Map.Entry<Object, Object>> iterator) {
      super(iterator);
    }

    @Override
    public INamedItem next() {
      final Map.Entry<?, ?> next = this.iterator.next();
      return new INamedItem() {
        @Override
        public String name() {
          return String.valueOf(next.getKey());
        }

        @Override
        public Object item() {
          return next.getValue();
        }

        @Override
        public String originalName() {
          return null;
        }

        @Override
        public Set<String> modifiers() {
          return Collections.emptySet();
        }
      };
    }
  }

  /**
   * Use the named iterator to name the items with the field names.
   */
  private static class NamedFieldIterator extends NamedIterable<Field> {

    private final Object target;
    private final HashSet<String> seenNames = new HashSet<>();

    public NamedFieldIterator(final Object target, final Iterator<Field> iterator) {
      super(iterator);
      this.target = target;
    }

    @Override
    public INamedItem next() {
      final Field next = this.iterator.next();
      final String name = getFieldName(next);
      seenNames.add(next.getName());
      return new INamedItem() {
        @Override
        public String name() {
          return name;
        }

        @Override
        public Object item() {
          return ReflectionUtils.callField(target, next);
        }

        @Override
        public String originalName() {
          // if we prefix the name with class name, then set the original name to the field name
          if (name.equals(next.getName())) {
            return null;
          }
          return next.getName();
        }

        @Override
        public Set<String> modifiers() {
          return ReflectionUtils.getModifiers(next);
        }
      };
    }

    private String getFieldName(final Field next) {
      final String name;
      // it is possible to have a field that is declared as a private value on a super type.
      // here we will prefix the declaring class name of the field if we have already seen the simple name
      if (seenNames.contains(next.getName())) {
        if (next.getDeclaringClass() == this.target.getClass()) {
          name = next.getName();
        } else {
          name = String.format("%s.%s", next.getDeclaringClass().getSimpleName(), next.getName());
        }
      } else {
        name = next.getName();
      }
      return name;
    }
  }

  /**
   * A simple iterator that used {@link ReflectionUtils} to create iterators for the {@link Field} that exist on an object. This allows us
   * to feed the fields into the {@link NamedFieldIterator}.
   */
  private static class FieldIterator implements Iterator<Field> {

    private final Class<?> clazz;
    private final Iterator<Field> iterator;

    public FieldIterator(final Class<?> clazz) {
      this.clazz = clazz;
      if (this.clazz.getSuperclass() == null || this.clazz.getSuperclass() == Object.class) {
        this.iterator = ReflectionUtils.getFieldIterator(clazz);
      } else {
        this.iterator = new CompoundIterator<>(ReflectionUtils.getFieldIterator(clazz),
            new FieldIterator(this.clazz.getSuperclass()));
      }
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Field next() {
      return iterator.next();
    }
  }
}
