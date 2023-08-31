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

public class FrameCollector extends VariableProcessor {

  protected final Settings settings;
  protected final IEvaluator evaluator;
  protected final Map<String, Object> variables;
  private final StackTraceElement[] stack;

  private final String jspSuffix;
  private final List<String> jspPackages;

  public FrameCollector(final Settings settings, final IEvaluator evaluator,
      final Map<String, Object> variables,
      final StackTraceElement[] stack) {
    this.settings = settings;
    this.evaluator = evaluator;
    this.variables = variables;
    this.stack = stack;
    this.jspSuffix = settings.getSettingAs("jsp.suffix", String.class);
    this.jspPackages = settings.getAsList("jsp.packages");
  }

  protected IFrameResult processFrame() {
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

  private StackFrame processFrame(final StackTraceElement stackTraceElement,
      final boolean collectVars,
      final int frameIndex) {
    final String className = stackTraceElement.getClassName();

    final boolean nativeMethod = stackTraceElement.isNativeMethod();
    final boolean appFrame = isAppFrame(stackTraceElement);
    final String methodName = getMethodName(stackTraceElement, variables, frameIndex);

    final Collection<VariableID> varIds;
    if (collectVars) {
      varIds = this.processVars(this.selectVariables(frameIndex));
    } else {
      varIds = Collections.emptyList();
    }
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

  private FileNameMapping getFileNameMapping(final StackTraceElement stackTraceElement, final String className) {

    if (!JSPUtils.isJspClass(jspSuffix, jspPackages, className)) {
      return new FileNameMapping(getFileName(stackTraceElement), stackTraceElement.getLineNumber(), null, -1);
    }

    Class<?> forName = null;
    try {
      forName = Class.forName(className);
    } catch (ClassNotFoundException ignored) {
      // not possible
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

  private static class FileNameMapping {

    public final String fileName;
    public final int lineNumber;
    private final String transpiledFile;
    private final int transpiledLine;

    public FileNameMapping(final String fileName, final int lineNumber, final String transpiledFile, final int transpiledLine) {
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.transpiledFile = transpiledFile;
      this.transpiledLine = transpiledLine;
    }
  }

  protected Map<String, Object> selectVariables(final int frameIndex) {
    if (frameIndex == 0) {
      return this.variables;
    }
    // todo can we use native to get variables up the stack
    return Collections.emptyMap();
  }

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

  protected boolean isAppFrame(final StackTraceElement stackTraceElement) {
    final List<String> inAppInclude = settings.getAsList("in.app.include");
    final List<String> inAppExclude = settings.getAsList("in.app.exclude");

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

  private String getFileName(final StackTraceElement stackTraceElement) {
    if (stackTraceElement.getFileName() == null) {
      return InstUtils.shortClassName(stackTraceElement.getClassName());
    }
    return Utils.trimPrefix(stackTraceElement.getFileName(), "/");
  }

  protected String getMethodName(final StackTraceElement stackTraceElement,
      final Map<String, Object> variables,
      final int frameIndex) {
    return stackTraceElement.getMethodName();
  }

  protected IExpressionResult evaluateWatchExpression(final String watch) {
    try {
      final Object result = this.evaluator.evaluateExpression(watch, this.variables);
      final List<VariableID> variableIds = processVars(Collections.singletonMap(watch, result));
      final Map<String, Variable> watchLookup = closeLookup();
      return new IExpressionResult() {
        @Override
        public WatchResult result() {
          return new WatchResult(watch, variableIds.get(0));
        }

        @Override
        public Map<String, Variable> variables() {
          return watchLookup;
        }
      };
    } catch (Throwable t) {
      return new IExpressionResult() {
        @Override
        public WatchResult result() {
          return new WatchResult(watch,
              String.format("%s: %s", t.getClass().getName(), t.getMessage()));
        }

        @Override
        public Map<String, Variable> variables() {
          return Collections.emptyMap();
        }
      };
    }
  }

  protected Resource processAttributes(final TracePointConfig tracepoint) {
    final HashMap<String, Object> attributes = new HashMap<>();
    attributes.put("tracepoint", tracepoint.getId());
    attributes.put("path", tracepoint.getPath());
    attributes.put("line", tracepoint.getLineNo());
    attributes.put("stack", tracepoint.getStackType());
    attributes.put("frame", tracepoint.getFrameType());

    if (!tracepoint.getWatches().isEmpty()) {
      attributes.put("has_watches", true);
    }

    if (tracepoint.getCondition() != null && !tracepoint.getCondition().trim().isEmpty()) {
      attributes.put("has_condition", true);
    }

    return Resource.create(attributes);
  }

  protected interface IFrameResult {

    Collection<StackFrame> frames();

    Map<String, Variable> variables();
  }

  protected interface IExpressionResult {

    WatchResult result();

    Map<String, Variable> variables();
  }
}
