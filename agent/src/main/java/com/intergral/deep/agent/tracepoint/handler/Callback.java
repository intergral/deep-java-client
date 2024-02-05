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
import com.intergral.deep.agent.api.plugin.ISnapshotContext;
import com.intergral.deep.agent.api.plugin.ISnapshotDecorator;
import com.intergral.deep.agent.api.plugin.ITraceProvider;
import com.intergral.deep.agent.api.plugin.ITraceProvider.ISpan;
import com.intergral.deep.agent.api.plugin.LazyEvaluator;
import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.push.PushService;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointConfigService;
import com.intergral.deep.agent.tracepoint.cf.CFEvaluator;
import com.intergral.deep.agent.tracepoint.cf.CFFrameProcessor;
import com.intergral.deep.agent.tracepoint.evaluator.EvaluatorService;
import com.intergral.deep.agent.tracepoint.handler.FrameProcessor.IFactory;
import com.intergral.deep.agent.types.TracePointConfig;
import com.intergral.deep.agent.types.TracePointConfig.EStage;
import com.intergral.deep.agent.types.snapshot.EventSnapshot;
import com.intergral.deep.agent.types.snapshot.Variable;
import com.intergral.deep.agent.types.snapshot.VariableID;
import com.intergral.deep.agent.types.snapshot.WatchResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type is the main entry point that is used to callback from injected code.
 */
public final class Callback {

  private Callback() {
  }

  private static final ThreadLocal<Deque<CallbackHook>> CALLBACKS = ThreadLocal.withInitial(ArrayDeque::new);
  private static final Logger LOGGER = LoggerFactory.getLogger(Callback.class);
  private static final ThreadLocal<Boolean> FIRING = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private static Settings SETTINGS;

  private static TracepointConfigService TRACEPOINT_SERVICE;
  private static PushService PUSH_SERVICE;
  private static int OFFSET;

  /**
   * Initialise the callback with the deep services.
   *
   * @param settings                the deep settings
   * @param tracepointConfigService the tracepoint service
   * @param pushService             the push service
   */
  public static void init(
      final Settings settings,
      final TracepointConfigService tracepointConfigService,
      final PushService pushService) {
    Callback.SETTINGS = settings;
    Callback.TRACEPOINT_SERVICE = tracepointConfigService;
    Callback.PUSH_SERVICE = pushService;
    // to avoid using the property Visitor.CALLBACK_CLASS (as this defaults to a java. class that makes tests complicated)
    final String property = System.getProperty("deep.callback.class");
    if (property == null || !property.equals(Callback.class.getName())) {
      Callback.OFFSET = 4;
    } else {
      Callback.OFFSET = 3;
    }
  }


  /**
   * The main entry point for CF ASM injected breakpoints.
   *
   * @param bpIds     the bp ids to trigger
   * @param filename  the filename of the breakpoint hit
   * @param lineNo    the line number of the breakpoint hit
   * @param variables the map of local variables.
   */
  public static void callBackCF(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> variables) {
    try {
      final IEvaluator evaluator = new LazyEvaluator(new CFEvaluator.Loader(variables));
      commonCallback(bpIds, filename, lineNo, null, variables, evaluator, CFFrameProcessor::new, null);
    } catch (Throwable t) {
      LOGGER.debug("Unable to process tracepoint {}:{}", filename, lineNo, t);
    }
  }


  /**
   * The main entry point for non CF ASM injected breakpoints.
   *
   * @param bpIds     the bp ids to trigger
   * @param filename  the filename of the breakpoint hit
   * @param lineNo    the line number of the breakpoint hit
   * @param variables the map of local variables.
   */
  public static void callBack(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> variables) {
    try {
      final IEvaluator evaluator = new LazyEvaluator(EvaluatorService::createEvaluator);
      final ICallbackResult callbackResult = commonCallback(bpIds, filename, lineNo, null, variables, evaluator,
          FrameProcessor::new, null);
      if (callbackResult != null) {
        final String spans = callbackResult.tracepoints().stream()
            .filter(tracePointConfig -> TracePointConfig.LINE.equals(tracePointConfig.getArg(TracePointConfig.SPAN, String.class, null)))
            .map(TracePointConfig::getId).collect(Collectors.joining(","));
        final Closeable span;
        if (!spans.isEmpty()) {
          span = span(filename + "#" + lineNo, spans);
        } else {
          span = null;
        }
        CALLBACKS.get().add(new CallbackHook(span, callbackResult.deferred(), callbackResult.frameProcessor(),
            callbackResult.frameProcessor().getLineStart()[1]));
      }
    } catch (Throwable t) {
      LOGGER.debug("Unable to process tracepoint {}:{}", filename, lineNo, t);
    }
  }


  private static ICallbackResult commonCallback(final List<String> tracepointIds,
      final String filename,
      final int lineNo,
      final String methodName,
      final Map<String, Object> variables,
      final IEvaluator evaluator,
      final IFactory factory,
      final CallbackHook callbackHook) {
    if (tracepointIds.isEmpty()) {
      if (callbackHook != null) {
        callbackHook.close(Callback.PUSH_SERVICE);
      }
      return null;
    }
    final long[] lineStart = Utils.currentTimeNanos();
    if (FIRING.get()) {
      LOGGER.debug("hit - skipping as we are already firing");
      return null;
    }

    try {
      FIRING.set(true);
      LOGGER.trace("callBack for {}:{} -> {}", filename, lineNo, tracepointIds);

      // possible race condition but unlikely
      if (Callback.TRACEPOINT_SERVICE == null) {
        return null;
      }

      final Collection<TracePointConfig> tracePointConfigs = Callback.TRACEPOINT_SERVICE.loadTracepointConfigs(
          tracepointIds);

      StackTraceElement[] stack = Thread.currentThread().getStackTrace();
      if (stack.length > OFFSET) {
        // Remove callBackProxy() + callBack() + commonCallback() + getStackTrace() entries to get to the real bp location
        stack = Arrays.copyOfRange(stack, OFFSET, stack.length);
      }

      final FrameProcessor frameProcessor = factory.provide(Callback.SETTINGS,
          evaluator,
          variables,
          tracePointConfigs,
          lineStart,
          stack,
          methodName);

      final ArrayList<EventSnapshot> deferredSnapshots = new ArrayList<>();
      if (frameProcessor.canCollect()) {
        frameProcessor.configureSelf();
        try {
          final Collection<EventSnapshot> collect = frameProcessor.collect();
          for (EventSnapshot eventSnapshot : collect) {
            decorate(eventSnapshot, frameProcessor);
            if (isDeferred(eventSnapshot)) {
              deferredSnapshots.add(eventSnapshot);
              continue;
            }
            // if we have a callback hook then decorate snapshot with details
            if (callbackHook != null) {
              callbackHook.decorate(eventSnapshot, frameProcessor);
            }
            Callback.PUSH_SERVICE.pushSnapshot(eventSnapshot);
          }
        } catch (Exception e) {
          LOGGER.debug("Error processing snapshot", e);
        }
      }

      if (callbackHook != null) {
        callbackHook.close(Callback.PUSH_SERVICE);
      }
      return new ICallbackResult() {
        @Override
        public Collection<EventSnapshot> deferred() {
          return deferredSnapshots;
        }

        @Override
        public Collection<TracePointConfig> tracepoints() {
          return frameProcessor.getFilteredTracepoints();
        }

        @Override
        public FrameProcessor frameProcessor() {
          frameProcessor.closeLookup();
          return frameProcessor;
        }
      };
    } finally {
      FIRING.set(false);
    }
  }

  private interface ICallbackResult {

    Collection<EventSnapshot> deferred();

    Collection<TracePointConfig> tracepoints();

    FrameProcessor frameProcessor();
  }

  private static boolean isDeferred(final EventSnapshot eventSnapshot) {
    final EStage arg = eventSnapshot.getTracepoint().getArg(TracePointConfig.STAGE, EStage.class, null);
    return arg == EStage.LINE_CAPTURE || arg == EStage.METHOD_CAPTURE;
  }

  private static void decorate(final EventSnapshot snapshot, final ISnapshotContext context) {
    final Collection<ISnapshotDecorator> plugins = Callback.SETTINGS.getPlugins(ISnapshotDecorator.class);
    for (ISnapshotDecorator plugin : plugins) {
      try {
        final Resource decorate = plugin.decorate(Callback.SETTINGS, context);
        if (decorate != null) {
          snapshot.mergeAttributes(decorate);
        }
      } catch (Throwable t) {
        LOGGER.error("Error processing plugin {}", plugin.getClass().getName());
      }
    }
    snapshot.close();
  }

  /**
   * This is called when an exception is captured on the visited line.
   *
   * @param t the exception that was captured.
   */
  public static void callBackException(final Throwable t) {
    try {
      LOGGER.debug("Capturing throwable", t);
      final CallbackHook element = CALLBACKS.get().peekLast();
      if (element != null) {
        element.setThrowable(t);
      }
    } catch (Throwable tt) {
      LOGGER.debug("Error processing callback", tt);
    }
  }

  /**
   * This is called when the visited line is completed.
   *
   * @param bpIds the tracepoint ids that triggered this
   * @param filename the source file name
   * @param lineNo the line number we are on
   * @param variables the captured local variables
   */
  public static void callBackFinally(final List<String> bpIds,
      final String filename,
      final int lineNo,
      final Map<String, Object> variables) {
    try {
      final IEvaluator evaluator = new LazyEvaluator(EvaluatorService::createEvaluator);
      final CallbackHook callbackHook = CALLBACKS.get().pollLast();
      commonCallback(bpIds, filename, lineNo, null, variables, evaluator, FrameProcessor::new, callbackHook);
    } catch (Throwable t) {
      LOGGER.debug("Unable to process tracepoint {}:{}", filename, lineNo, t);
    }
  }

  /**
   * Create a span using the tracepoint callback.
   * <p>
   * This method will <b>Always</b> return a closable. This way the injected code never deals with anything but calling close. Even if close
   * doesn't do anything.
   * <p>
   * We use {@link Closeable} here, so we can stick to java types in the injected code. This makes testing and injected code simpler.
   *
   * @param name the name of the span
   * @param bps  the tracepoints that triggered this span
   * @return a {@link Closeable} to close the span
   */
  public static Closeable span(final String name, final String bps) {
    try {
      final ITraceProvider plugin = SETTINGS.getPlugin(ITraceProvider.class);
      if (plugin == null) {
        return () -> {
        };
      }
      final ISpan span = plugin.createSpan(name);

      if (span == null) {
        return () -> {
        };
      }
      span.addAttribute("tracepoint", bps);
      return () -> {
        try {
          span.close();
        } catch (Throwable t) {
          LOGGER.error("Cannot close span: {}", name, t);
        }
      };
    } catch (Throwable t) {
      LOGGER.error("Cannot create span: {}", name, t);
      return () -> {
      };
    }
  }

  /**
   * This method is called when a tracepoint has triggered a method entry type.
   * <p>
   * This method will <b>Always</b> return a closable. This way the injected code never deals with anything but calling close. Even if close
   * doesn't do anything.
   * <p>
   * We use {@link Closeable} here, so we can stick to java types in the injected code. This makes testing and injected code simpler.
   *
   * @param methodName the method name we have entered
   * @param filename   the file name the method is in
   * @param lineNo     the line number the method is on
   * @param bpIds      the tracepoint ids that have been triggered by this entry
   * @param variables  the map of variables captured
   * @param spanOnlyIds the CSV of the tracepoints ids that just want a span
   */
  public static void methodEntry(final String methodName, final String filename, final int lineNo, final List<String> bpIds,
      final Map<String, Object> variables, final String spanOnlyIds) {
    try {
      final IEvaluator evaluator = new LazyEvaluator(EvaluatorService::createEvaluator);
      final ICallbackResult callbackResult = commonCallback(bpIds, filename, lineNo, methodName, variables, evaluator,
          FrameProcessor::new, null);
      if (callbackResult != null) {
        final String spans = callbackResult.tracepoints().stream()
            .filter(tracePointConfig -> TracePointConfig.METHOD.equals(tracePointConfig.getArg(TracePointConfig.SPAN, String.class, null)))
            .map(TracePointConfig::getId).collect(Collectors.joining(","));
        final Closeable span;
        if (!spans.isEmpty()) {
          span = span(methodName, spans + "," + spanOnlyIds);
        } else {
          span = null;
        }
        CALLBACKS.get().add(new CallbackHook(span, callbackResult.deferred(), callbackResult.frameProcessor(),
            callbackResult.frameProcessor().getLineStart()[1]));
      } else if (spanOnlyIds != null && !spanOnlyIds.isEmpty()) {
        final Closeable span = span(methodName, spanOnlyIds);
        CALLBACKS.get().add(new CallbackHook(span, Collections.emptyList(), null, Utils.currentTimeNanos()[1]));
      } else {
        // we need a callback as we need to capture error/return
        // we would only get this far if we are a method exit capture tp
        CALLBACKS.get().add(new CallbackHook(null, Collections.emptyList(), null, Utils.currentTimeNanos()[1]));
      }

    } catch (Throwable t) {
      LOGGER.debug("Unable to process tracepoint {}:{}", filename, lineNo, t);
    }
  }

  /**
   * This is called when an exception is captured from a wrapped method.
   *
   * @param t the captured throwable
   */
  public static void methodException(final Throwable t) {
    try {
      LOGGER.debug("Capturing throwable", t);
      final CallbackHook element = CALLBACKS.get().peekLast();
      if (element != null) {
        element.setThrowable(t);
      }
    } catch (Throwable tt) {
      LOGGER.debug("Error processing callback", tt);
    }
  }

  /**
   * This is called when the returned value from the wrapped method is captured.
   * <p>
   * This method is not called on void methods.
   *
   * @param value the captured return value.
   */
  public static void methodRet(final Object value) {
    try {
      LOGGER.debug("Capturing ret: {}", value);
      final CallbackHook element = CALLBACKS.get().peekLast();
      if (element != null) {
        element.setReturn(value);
      }
    } catch (Throwable tt) {
      LOGGER.debug("Error processing callback", tt);
    }
  }

  /**
   * This method is called when a wrapped method has completed.
   *
   * @param methodName the method name
   * @param filename the source file name
   * @param lineNo the line number
   * @param bpIds the triggering tracepoints ids
   * @param variables the captured local variables
   */
  public static void methodEnd(final String methodName, final String filename, final int lineNo, final List<String> bpIds,
      final Map<String, Object> variables) {
    try {
      final IEvaluator evaluator = new LazyEvaluator(EvaluatorService::createEvaluator);
      final CallbackHook callbackHook = CALLBACKS.get().pollLast();
      commonCallback(bpIds, filename, lineNo, methodName, variables, evaluator, FrameProcessor::new, callbackHook);
    } catch (Throwable t) {
      LOGGER.debug("Unable to process tracepoint {}:{}", filename, lineNo, t);
    }
  }

  private static class CallbackHook {

    private final Closeable span;
    private final Collection<EventSnapshot> deferred;
    private final FrameProcessor frameProcessor;
    private final long ts;
    private Throwable throwable;
    private boolean isSet = false;
    private Object value;

    public CallbackHook(final Closeable span, final Collection<EventSnapshot> deferred, final FrameProcessor frameProcessor,
        final long ts) {
      this.span = span;
      this.deferred = deferred;
      this.frameProcessor = frameProcessor;
      this.ts = ts;
    }

    public void setThrowable(final Throwable throwable) {
      this.throwable = throwable;
    }

    public void setReturn(final Object value) {
      isSet = true;
      this.value = value;
    }

    public void close(final PushService pushService) {
      if (span != null) {
        try {
          span.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      for (EventSnapshot eventSnapshot : deferred) {
        decorate(eventSnapshot, frameProcessor);
        pushService.pushSnapshot(eventSnapshot);
      }
    }

    public void decorate(final EventSnapshot eventSnapshot, final FrameProcessor frameProcessor) {
      final long nanos = Utils.currentTimeNanos()[1];
      final long durationNs = nanos - this.ts;
      if (this.throwable != null) {
        final List<VariableID> variableIds = frameProcessor.processVars(Collections.singletonMap("thrown", this.throwable));
        final Map<String, Variable> watchLookup = frameProcessor.closeLookup();
        eventSnapshot.addWatchResult(new WatchResult("thrown", variableIds.get(0), WatchResult.WATCH));
        eventSnapshot.mergeVariables(watchLookup);
      }

      if (this.isSet) {
        final List<VariableID> variableIds = frameProcessor.processVars(Collections.singletonMap("return", this.value));
        final Map<String, Variable> watchLookup = frameProcessor.closeLookup();
        eventSnapshot.addWatchResult(new WatchResult("return", variableIds.get(0), WatchResult.WATCH));
        eventSnapshot.mergeVariables(watchLookup);
      }

      final List<VariableID> variableIds = frameProcessor.processVars(Collections.singletonMap("runtime", durationNs));
      final Map<String, Variable> watchLookup = frameProcessor.closeLookup();
      eventSnapshot.addWatchResult(new WatchResult("runtime", variableIds.get(0), WatchResult.WATCH));
      eventSnapshot.mergeVariables(watchLookup);
    }
  }
}
