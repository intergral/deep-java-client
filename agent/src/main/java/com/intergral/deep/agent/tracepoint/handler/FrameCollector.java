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

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.api.plugin.IEvaluator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;
import com.intergral.deep.agent.api.utils.string.StringSubstitutor;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.handler.bfs.Node;
import com.intergral.deep.agent.tracepoint.inst.InstUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.JSPUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMap;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMapLookup;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.snapshot.StackFrame;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.agent.types.snapshot.WatchResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This type allows the collection of frame data, ie stack frames, watchers and other tracepoint data.
 */
public class FrameCollector extends VariableProcessor {

  /**
   * The current settings use my deep.
   */
  protected final Settings settings;

  /**
   * The evaluator that should be used by this callback.
   */
  protected final IEvaluator evaluator;

  /**
   * The variables that have been captured by the callback.
   */
  protected final Map<String, Object> variables;

  /**
   * The stack trace elements captured by this callback.
   */
  private final StackTraceElement[] stack;

  /**
   * We cache this value in the constructor to reduce look up later.
   */
  private final String jspSuffix;

  /**
   * We cache this value in the constructor to reduce look up later.
   */
  private final List<String> jspPackages;

  /**
   * The name of the method we are wrapping, or {@code null} if not a method wrapped collection.
   */
  private final String methodName;

  /**
   * Create a frame collector to collect the frame data.
   *
   * @param settings  the current settings being used by deep
   * @param evaluator the evaluator to use for this callback
   * @param variables the variables captured by the callback
   * @param stack     the stack captured by the callback
   * @param methodName the name of the method we are wrapping, or {@code null} if not a method wrapped collection
   */
  public FrameCollector(
      final Settings settings,
      final IEvaluator evaluator,
      final Map<String, Object> variables,
      final StackTraceElement[] stack,
      final String methodName) {
    this.settings = settings;
    this.evaluator = evaluator;
    this.variables = variables;
    this.stack = stack;
    this.jspSuffix = settings.getSettingAs(ISettings.JSP_SUFFIX, String.class);
    this.jspPackages = settings.getAsList(ISettings.JSP_PACKAGES);
    this.methodName = methodName;
  }

  /**
   * Processes and collects all the data for the captured frame.
   *
   * @return the result of the process {@link IFrameResult}
   */
  protected IFrameResult processFrame() {
    // if all tracepoints are no collect then skip collection
    if (this.frameConfig.isNoCollect()) {
      return new IFrameResult() {
        @Override
        public Collection<StackFrame> frames() {
          return Collections.emptyList();
        }

        @Override
        public Map<String, Variable> variables() {
          return Collections.emptyMap();
        }
      };
    }

    final ArrayList<StackFrame> frames = new ArrayList<>();

    for (int i = 0; i < stack.length; i++) {
      final StackTraceElement stackTraceElement = stack[i];
      final StackFrame frame = this.processFrame(stackTraceElement,
          this.frameConfig.shouldCollectVars(i), i);
      frames.add(frame);
    }

    final Map<String, Variable> closedLookup = closeLookup();

    return new IFrameResult() {
      @Override
      public Collection<StackFrame> frames() {
        return frames;
      }

      @Override
      public Map<String, Variable> variables() {
        if (closedLookup == null) {
          return Collections.emptyMap();
        }
        return closedLookup;
      }
    };
  }

  /**
   * Process an individual stack frame into a {@link StackFrame} including the variables, if this frame should collect variables.
   *
   * @param stackTraceElement the stack frame to collect
   * @param collectVars       {@code true} if we should collect variables
   * @param frameIndex        the index of the frame we should collect, index {@code 0} indicates the highest frame (e.g. current executing
   *                          frame)
   * @return the result of the frame as {@link StackFrame}
   */
  private StackFrame processFrame(
      final StackTraceElement stackTraceElement,
      final boolean collectVars,
      final int frameIndex) {

    final String className = stackTraceElement.getClassName();
    final boolean nativeMethod = stackTraceElement.isNativeMethod();
    final boolean appFrame = isAppFrame(stackTraceElement);
    // we use a method to get the method name, so we can override it in CF (as CF uses different method names)
    final String methodName = getMethodName(stackTraceElement, variables, frameIndex);

    final Collection<VariableID> varIds;
    if (collectVars) {
      varIds = this.processVars(this.selectVariables(frameIndex));
    } else {
      varIds = Collections.emptyList();
    }
    // map the file name - this deals with transpiled jsp source files
    final FileNameMapping mapping = getFileNameMapping(stackTraceElement, className);

    return new StackFrame(mapping.fileName,
        mapping.lineNumber,
        className,
        methodName,
        appFrame,
        nativeMethod,
        varIds,
        mapping.transpiledFile,
        mapping.transpiledLine);
  }

  /**
   * We need to be careful when selecting the file name as it is not always available.
   * <ul>
   *   <li>native frames do not have file names, or line numbers</li>
   *   <li>JSP classes can be mad up of multiple source files so we need to use the {@link SourceMap}</li>
   *   <li>it is possible to not include source names when compiling Java classes</li>
   * </ul>
   *
   * @param stackTraceElement the stack frame element
   * @param className         the classname
   * @return the mapped file name and line numbers
   */
  private FileNameMapping getFileNameMapping(final StackTraceElement stackTraceElement, final String className) {

    if (!JSPUtils.isJspClass(jspSuffix, jspPackages, className)) {
      return new FileNameMapping(getFileName(stackTraceElement), stackTraceElement.getLineNumber(), null, -1);
    }

    Class<?> forName = null;
    try {
      forName = Class.forName(className);
    } catch (ClassNotFoundException ignored) {
      // not possible
      // we are literally executing the code, so the class has to be loaded
      // in theory this could happen with some complex class loaders, but it shouldn't
      // if it does there is nothing we can do anyway
    }

    if (forName == null) {
      return new FileNameMapping(getFileName(stackTraceElement), stackTraceElement.getLineNumber(), null, -1);
    }

    final SourceMap sourceMap = JSPUtils.getSourceMap(forName);
    if (sourceMap == null) {
      return new FileNameMapping(getFileName(stackTraceElement), stackTraceElement.getLineNumber(), null, -1);
    }

    final SourceMapLookup lookup = sourceMap.lookup(stackTraceElement.getLineNumber());
    final String jspFilename = lookup.getFilename();
    final int jspLine = lookup.getLineNumber();
    final String transpiledFile = getFileName(stackTraceElement);
    final int transpiledLine = stackTraceElement.getLineNumber();
    return new FileNameMapping(jspFilename, jspLine, transpiledFile, transpiledLine);
  }

  /**
   * Select from the available captured variables the variables we want to process for this frame. This is mainly here to allow for an easy
   * way for CF to map the variables from their capture Java types to the expected CF types.
   *
   * @param frameIndex the index of the frame we are processing
   * @return the variables available at this frame
   */
  protected Map<String, Object> selectVariables(final int frameIndex) {
    if (frameIndex == 0) {
      return this.variables;
    }
    // todo can we use native to get variables up the stack
    return Collections.emptyMap();
  }

  /**
   * This is where we start the Breadth first search (BFS) of the selected variables.
   * <p>
   * Here we are essentially dealing with the BFS nodes and linking to the VariableProcessor to do the processing.
   *
   * @param variables the variables to process
   * @return the variable ref used in the snapshot
   * @see VariableProcessor
   */
  protected List<VariableID> processVars(final Map<String, Object> variables) {
    final List<VariableID> frameVars = new ArrayList<>();

    final Node.IParent frameParent = frameVars::add;

    final Set<Node> initialNodes = variables.entrySet()
        .stream()
        .map(stringObjectEntry -> new Node(new Node.NodeValue(stringObjectEntry.getKey(),
            stringObjectEntry.getValue()), frameParent)).collect(Collectors.toSet());

    Node.breadthFirstSearch(new Node(null, new HashSet<>(initialNodes), frameParent),
        this::processNode);

    return frameVars;
  }

  /**
   * This is where we take a node from BFS queue and process it back onto the queue.
   * <p>
   * Essentially, we take a node, we process this node, the add the child nodes back onto the BFS queue.
   *
   * @param node the node to process
   * @return {@code true} if we should continue to process more nodes, else {@code false}.
   */
  protected boolean processNode(final Node node) {
    if (!this.checkVarCount()) {
      // we have exceeded the var count, so do not continue
      return false;
    }

    final Node.NodeValue value = node.getValue();
    if (value == null) {
      // this node has no value, continue with children
      return true;
    }

    // process this node variable
    final VariableResponse processResult = super.processVariable(value);
    final VariableID variableId = processResult.getVariableId();

    // add the result to the parent - this maintains the hierarchy in the var look up
    node.getParent().addChild(variableId);

    if (value.getValue() != null && processResult.processChildren()) {
      final Set<Node> childNodes = super.processChildNodes(variableId, value.getValue(),
          node.depth());
      node.addChildren(childNodes);
    }
    return true;
  }

  /**
   * An app frame is defined via the settings {@link ISettings#APP_FRAMES_INCLUDES} and  {@link ISettings#APP_FRAMES_EXCLUDES}. This gives a
   * way to tell deep that the frame is part of your app and not part of the framework. This is primarily used as a way to filter frames in
   * the UI.
   *
   * @param stackTraceElement the stack frame to process
   * @return {@code true} if the class name is in the included packages, and not in the excluded packages, else {@code false}.
   */
  protected boolean isAppFrame(final StackTraceElement stackTraceElement) {
    final List<String> inAppInclude = settings.getAsList(ISettings.APP_FRAMES_INCLUDES);
    final List<String> inAppExclude = settings.getAsList(ISettings.APP_FRAMES_EXCLUDES);

    final String className = stackTraceElement.getClassName();

    for (String exclude : inAppExclude) {
      if (className.equals(exclude)) {
        return false;
      }
    }

    for (String include : inAppInclude) {
      if (className.equals(include)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the file name from the stack trace element, as we always need a file name. If there is not a file name then use the short class
   * name.
   *
   * @param stackTraceElement the frame to process
   * @return the file name, or class name
   */
  private String getFileName(final StackTraceElement stackTraceElement) {
    if (stackTraceElement.getFileName() == null) {
      return InstUtils.shortClassName(stackTraceElement.getClassName());
    }
    return Utils.trimPrefix(stackTraceElement.getFileName(), "/");
  }

  /**
   * Get the method name from the stack frame.
   *
   * @param stackTraceElement the stack frame to process
   * @param variables         the variables for the frame
   * @param frameIndex        the frame index
   * @return the name of the method
   */
  protected String getMethodName(final StackTraceElement stackTraceElement,
      final Map<String, Object> variables,
      final int frameIndex) {
    return stackTraceElement.getMethodName();
  }

  /**
   * Evaluate a watch expression into a {@link IExpressionResult}.
   * <p>
   * We always need a result from a watch expression, however, it is possible to have bad watches that error. In some cases it is possible
   * to not have a valid evaluator.
   * <p>
   * So if we cannot get a result from the {@link IEvaluator} then we return an error result.
   *
   * @param watch  the watch expression to evaluate
   * @param source the source of the watch expression
   * @return a {@link IExpressionResult}
   * @see WatchResult#LOG
   * @see WatchResult#METRIC
   * @see WatchResult#WATCH
   */
  protected IExpressionResult evaluateWatchExpression(final String watch, final String source) {
    try {
      final Object result = this.evaluator.evaluateExpression(watch, this.variables);
      final List<VariableID> variableIds = processVars(Collections.singletonMap(watch, result));
      final Map<String, Variable> watchLookup = closeLookup();
      return new IExpressionResult() {
        @Override
        public WatchResult result() {
          return new WatchResult(watch, variableIds.get(0), source);
        }

        @Override
        public Map<String, Variable> variables() {
          return watchLookup;
        }

        @Override
        public String logString() {
          return Utils.valueOf(result);
        }

        @Override
        public Number numberValue() {
          if (result instanceof Number) {
            // todo should we wrap this is new Number() ?
            return (Number) result;
          }
          final String string = Utils.valueOf(result);
          try {
            return Double.valueOf(string);
          } catch (NumberFormatException nfe) {
            return Double.NaN;
          }
        }

        @Override
        public boolean isError() {
          return false;
        }
      };
    } catch (Throwable t) {
      return new IExpressionResult() {
        @Override
        public WatchResult result() {
          return new WatchResult(watch,
              String.format("%s: %s", t.getClass().getName(), t.getMessage()), source);
        }

        @Override
        public Map<String, Variable> variables() {
          return Collections.emptyMap();
        }

        @Override
        public String logString() {
          return Utils.throwableToString(t);
        }

        @Override
        public Number numberValue() {
          return Double.NaN;
        }

        @Override
        public boolean isError() {
          return true;
        }
      };
    }
  }

  /**
   * Using the current tracepoint config, create a {@link Resource} the can be used as the attributes.
   * <p>
   * The basic attributes created are:
   * <ul>
   *   <li>tracepoint - the id of the tracepoint</li>
   *   <li>path - the path of the tracepoint</li>
   *   <li>line - the line of the tracepoint</li>
   *   <li>stack - the stack type of the tracepoint</li>
   *   <li>frame - the frame type of the tracepoint</li>
   *   <li>has_watchers - boolean indicating there are watchers</li>
   *   <li>has_condition - boolean indicating there is a condition</li>
   * </ul>
   *
   * @param tracepoint the current config
   * @return the new {@link Resource}
   */
  protected Resource processAttributes(final TracePointConfig tracepoint) {
    final HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("tracepoint", tracepoint.getId());
    attributes.put("path", tracepoint.getPath());
    attributes.put("line", tracepoint.getLineNo());
    attributes.put("stack", tracepoint.getStackType());
    attributes.put("frame", tracepoint.getFrameType());

    if (this.methodName != null) {
      attributes.put("method_name", this.methodName);
    }

    if (!tracepoint.getWatches().isEmpty()) {
      attributes.put("has_watches", true);
    }

    if (tracepoint.getCondition() != null && !tracepoint.getCondition().trim().isEmpty()) {
      attributes.put("has_condition", true);
    }

    return Resource.create(attributes);
  }

  protected ILogProcessResult processLogMsg(final TracePointConfig tracepoint, final String logMsg) {

    final ArrayList<WatchResult> watchResults = new ArrayList<>();
    final HashMap<String, Variable> variables = new HashMap<>();
    final String processedLog = processSubstitution(logMsg, watchResults, variables);

    return new ILogProcessResult() {
      @Override
      public String processedLog() {
        return "[deep] " + processedLog;
      }

      @Override
      public Collection<WatchResult> result() {
        return watchResults;
      }

      @Override
      public Map<String, Variable> variables() {
        return variables;
      }
    };
  }

  private String processSubstitution(final String logMsg, final ArrayList<WatchResult> watchResults,
      final HashMap<String, Variable> variables) {
    final StringSubstitutor stringSubstitutor = new StringSubstitutor(key -> {
      final IExpressionResult iExpressionResult = evaluateWatchExpression(key, WatchResult.LOG);
      watchResults.add(iExpressionResult.result());
      variables.putAll(iExpressionResult.variables());
      return iExpressionResult.logString();
    });
    stringSubstitutor.setDisableSubstitutionInValues(true);
    stringSubstitutor.setEnableSubstitutionInVariables(false);

    return stringSubstitutor.replace(logMsg);
  }

  protected void logTracepoint(final String logMsg, final String tracepointId, final String snapshotId) {
    this.settings.logTracepoint(logMsg, tracepointId, snapshotId);
  }

  /**
   * The result of processing the tracepoint log message.
   */
  protected interface ILogProcessResult {

    String processedLog();

    Collection<WatchResult> result();

    Map<String, Variable> variables();
  }

  /**
   * The result of processing the frames.
   *
   * @see #processFrame()
   */
  protected interface IFrameResult {

    Collection<StackFrame> frames();

    Map<String, Variable> variables();
  }

  /**
   * The result of evaluating an expression.
   *
   * @see #evaluateWatchExpression(String, String)
   */
  protected interface IExpressionResult {

    WatchResult result();

    Map<String, Variable> variables();

    String logString();

    Number numberValue();

    boolean isError();
  }

  /**
   * A small type to wrap the mapped file and line numbers into an easy return.
   */
  private static class FileNameMapping {

    /**
     * Should always have a value. Will be the source file name if available, or the class name if not.
     * <p>
     * If the class being processed is a transpiled type, e.g. JSP then this will be the original source file.
     */
    private final String fileName;
    /**
     * Can be negative.
     * <p>
     * If the class being processed is a transpiled type, e.g. JSP then this will be the original source file line number.
     *
     * @see StackTraceElement#getLineNumber()
     */
    private final int lineNumber;

    /**
     * If the class being processed is a transpiled type, e.g. JSP then this will be the filename before it is mapped.
     */
    private final String transpiledFile;
    /**
     * Can be negative.
     * <p>
     * If the class being processed is a transpiled type, e.g. JSP then this will be the linenumber before it is mapped.
     *
     * @see StackTraceElement#getLineNumber()
     */
    private final int transpiledLine;

    public FileNameMapping(final String fileName, final int lineNumber, final String transpiledFile, final int transpiledLine) {
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.transpiledFile = transpiledFile;
      this.transpiledLine = transpiledLine;
    }
  }
}
