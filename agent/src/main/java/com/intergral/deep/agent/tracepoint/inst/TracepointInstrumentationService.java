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

package com.intergral.deep.agent.tracepoint.inst;

import com.intergral.deep.agent.Utils;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.TracepointUtils;
import com.intergral.deep.agent.tracepoint.cf.CFUtils;
import com.intergral.deep.agent.tracepoint.inst.asm.ClassLoaderAwareClassWriter;
import com.intergral.deep.agent.tracepoint.inst.asm.SkipException;
import com.intergral.deep.agent.tracepoint.inst.asm.TransformerUtils;
import com.intergral.deep.agent.tracepoint.inst.asm.Visitor;
import com.intergral.deep.agent.tracepoint.inst.jsp.JSPMappedBreakpoint;
import com.intergral.deep.agent.tracepoint.inst.jsp.JSPUtils;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMap;
import com.intergral.deep.agent.tracepoint.inst.jsp.sourcemap.SourceMapLineStartEnd;
import com.intergral.deep.agent.types.TracePointConfig;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracepointInstrumentationService implements ClassFileTransformer {

  public static final long COMPUTE_ON_CLASS_VERSION = Long.getLong("nv.compute.class.version", 50L);
  private static final Logger LOGGER = LoggerFactory.getLogger(
      TracepointInstrumentationService.class);

  private final Instrumentation inst;
  private final String disPath;
  private final String jspSuffix;
  private final List<String> jspPackages;

  /**
   * We process JSP classes specially, so we collect them all under this class key
   */
  private final String JSP_CLASS_KEY = "jsp";

  /**
   * We process CFM classes specially, so we collect them all under this class key
   */
  private final String CFM_CLASS_KEY = "cfm";

  private Map<String, Map<String, TracePointConfig>> classPrefixTracepoints = new ConcurrentHashMap<>();


  public TracepointInstrumentationService(final Instrumentation inst, final Settings settings) {
    this.inst = inst;
    this.disPath = settings.getSettingAs("transform.path", String.class);
    //noinspection unchecked
    this.jspPackages = settings.getSettingAs("jsp.packages", List.class);
    this.jspSuffix = settings.getSettingAs("jsp.suffix", String.class);
  }

  public static TracepointInstrumentationService init(final Instrumentation inst,
      final Settings settings) {
    final TracepointInstrumentationService tracepointInstrumentationService = new TracepointInstrumentationService(
        inst,
        settings);

    inst.addTransformer(tracepointInstrumentationService, true);

    return tracepointInstrumentationService;
  }

  /**
   * Process the new config from the services and determine which classes need to be transformed,
   * and trigger transformation.
   *
   * @param breakpointResponse the new list of tracepoints that have been received from the server.
   */
  public synchronized void processBreakpoints(
      final Collection<TracePointConfig> breakpointResponse) {
    // keep record of existing tracepoints
    final Map<String, Map<String, TracePointConfig>> existingTracepoints = this.classPrefixTracepoints;
    final Set<String> newTracepointOnExistingClasses = new HashSet<>();
    final Map<String, Map<String, TracePointConfig>> newBreakpoints = new HashMap<>();

    // process new breakpoints mapping to new breakpoints map { className -> { breakpoint id -> breakpoint } }
    for (final TracePointConfig tracePointConfig : breakpointResponse) {
      final String fullClass = TracepointUtils.estimatedClassRoot(tracePointConfig);
      if (newBreakpoints.containsKey(fullClass)) {
        newBreakpoints.get(fullClass).put(tracePointConfig.getId(), tracePointConfig);
      } else {
        final HashMap<String, TracePointConfig> value = new HashMap<>();
        value.put(tracePointConfig.getId(), tracePointConfig);

        newBreakpoints.put(fullClass, value);
      }
      // track new tracepoints added to class that already has tracepoints on it
      final Map<String, TracePointConfig> existingConfig = existingTracepoints.get(fullClass);
      if (existingConfig != null && !existingConfig.containsKey(tracePointConfig.getId())) {
        newTracepointOnExistingClasses.add(fullClass);
      }
    }
    // set the new config of the tracepoints
    this.classPrefixTracepoints = new ConcurrentHashMap<>(newBreakpoints);

    // build class scanners
    final CompositeClassScanner compositeClassScanner = new CompositeClassScanner();

    // scanner to handle classes that no longer have classes and need transformed
    final IClassScanner removedTracepoints = reTransformClassesThatNoLongerHaveTracePoints(
        new HashSet<>(existingTracepoints.keySet()), new HashSet<>(newBreakpoints.keySet()));
    compositeClassScanner.addScanner(removedTracepoints);

    // scanner to handle classes that now have tracepoints and need transformed
    final IClassScanner newClasses = reTransformClassesThatAreNew(
        new HashSet<>(existingTracepoints.keySet()),
        new HashSet<>(newBreakpoints.keySet()));
    compositeClassScanner.addScanner(newClasses);

    // scanner to handle classes that have tracepoints already, but the configs have changed
    final SetClassScanner modifiedClasses = new SetClassScanner(newTracepointOnExistingClasses);
    compositeClassScanner.addScanner(modifiedClasses);

    // scanner to handle JSP classes
    if (this.classPrefixTracepoints.containsKey(JSP_CLASS_KEY) || existingTracepoints.containsKey(JSP_CLASS_KEY)) {
      final Map<String, TracePointConfig> jsp = this.classPrefixTracepoints.get(this.JSP_CLASS_KEY);
      final IClassScanner jspScanner = reTransFormJSPClasses(Utils.newMap(jsp),
          Utils.newMap(existingTracepoints.get(this.JSP_CLASS_KEY)));
      compositeClassScanner.addScanner(jspScanner);
    }

    // scanner to handle CFM classes
    if (this.classPrefixTracepoints.containsKey(CFM_CLASS_KEY) || existingTracepoints.containsKey(CFM_CLASS_KEY)) {
      final Map<String, TracePointConfig> cfm = this.classPrefixTracepoints.get(CFM_CLASS_KEY);
      final IClassScanner cfmScanner = reTransFormCfClasses(Utils.newMap(cfm),
          Utils.newMap(existingTracepoints.get(CFM_CLASS_KEY)));
      compositeClassScanner.addScanner(cfmScanner);
    }
    LOGGER.debug("New breakpoint config {}", this.classPrefixTracepoints);

    try {
      // scan loaded classes and transform
      final Class<?>[] classes = compositeClassScanner.scanAll(inst);
      if (classes.length != 0) {
        // TODO: 15.07.20 look at redefineClasses
        inst.retransformClasses(classes);
      }
    } catch (Throwable e) {
      LOGGER.error("Error re-transforming class", e);
    }
  }

  /**
   * Calculate the classes to scan for JSP
   *
   * @param newJSPState      the new JSP state
   * @param previousJSPState the previous JSP state
   * @return the JSP class scanner
   */
  private IClassScanner reTransFormJSPClasses(
      final Map<String, TracePointConfig> newJSPState,
      final Map<String, TracePointConfig> previousJSPState) {
    final Map<String, TracePointConfig> allTracepoints = withRemoved(newJSPState, previousJSPState);
    return new JSPClassScanner(allTracepoints, this.jspSuffix, this.jspPackages);
  }

  /**
   * Add the removed tracepoints info the new state.
   *
   * @param newState      the new state of tracepoints
   * @param previousState the previous state of tracepoints
   * @return the newState plus any previous that have been removed
   */
  private Map<String, TracePointConfig> withRemoved(
      final Map<String, TracePointConfig> newState,
      final Map<String, TracePointConfig> previousState) {
    final Set<String> newIds = newState.keySet();
    final Set<String> oldIds = previousState.keySet();

    oldIds.removeAll(newIds);

    for (String oldId : oldIds) {
      newState.put(oldId, previousState.get(oldId));
    }

    return newState;
  }

  /**
   * Calculate the classes to scan for CFM
   *
   * @param newCFMState      the new CFM state
   * @param previousCFMState the previous CFM state
   * @return the CFM class scanner
   */
  //exposed for testing
  protected CFClassScanner reTransFormCfClasses(
      final Map<String, TracePointConfig> newCFMState,
      final Map<String, TracePointConfig> previousCFMState) {
    final Map<String, TracePointConfig> allTracepoints = withRemoved(newCFMState, previousCFMState);

    return new CFClassScanner(allTracepoints);
  }

  private URL getLocation(final ProtectionDomain protectionDomain) {
    return protectionDomain.getCodeSource().getLocation();
  }

  /**
   * Calculate the classes that now have tracepoints but did not before.
   * <p>
   * e.g. If the classes {a, b, c} are the previous set and {a, d, e} is the new set. The result would be a set of {d,e}.
   *
   * @param previousState the list of classes that had tracepoints on them
   * @param newState      the list of classes that now have tracepoints on them
   * @return the set difference between the new and new previous
   */
  private IClassScanner reTransformClassesThatAreNew(final Set<String> previousState,
      final Set<String> newState) {
    newState.removeAll(previousState);
    // jsp and cfm are processed special so ignore them here
    previousState.remove(JSP_CLASS_KEY);
    previousState.remove(CFM_CLASS_KEY);
    return new SetClassScanner(newState);
  }

  /**
   * Calculate the classes that did have tracepoints but no longer do.
   * <p>
   * e.g. If the classes {a, b, c} are the previous set and {a, d, e} is the new set. The result would be a set of {b,c}.
   *
   * @param previousState the list of classes that had tracepoints on them
   * @param newState      the list of classes that now have tracepoints on them
   * @return the set difference between the previous and new state
   */
  private IClassScanner reTransformClassesThatNoLongerHaveTracePoints(
      final Set<String> previousState,
      final Set<String> newState) {
    previousState.removeAll(newState);
    // jsp and cfm are processed special so ignore them here
    previousState.remove(JSP_CLASS_KEY);
    previousState.remove(CFM_CLASS_KEY);
    return new SetClassScanner(previousState);
  }

  @Override
  public byte[] transform(final ClassLoader loader,
      final String classNameP,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classfileBuffer) {
    final boolean isCf;
    ClassReader reader = null;
    ClassNode cn = null;
    final Collection<TracePointConfig> iBreakpoints;
    final String className = InstUtils.internalClassStripInner(classNameP);
    final String shortClassName = InstUtils.fileName(
        className); // we use the method fileName as it strips all but the last of the internal class name for us
    final Collection<TracePointConfig> matchedTracepoints = matchTracepoints(className,
        shortClassName);
    // no breakpoints for this class or any CF classes
    if (matchedTracepoints.isEmpty() && !this.classPrefixTracepoints.containsKey(CFM_CLASS_KEY)
        && !this.classPrefixTracepoints.containsKey(JSP_CLASS_KEY)) {
      return null;
    }
    // no breakpoints for this class, but we have a cfm breakpoints, and this is a cfm class
    else if (matchedTracepoints.isEmpty()
        && this.classPrefixTracepoints.containsKey(CFM_CLASS_KEY)
        && CFUtils.isCfClass(classNameP)) {
      final Map<String, TracePointConfig> cfm = this.classPrefixTracepoints.get(CFM_CLASS_KEY);
      final URL location = getLocation(protectionDomain);
      if (location == null) {
        reader = new ClassReader(classfileBuffer);
        cn = new ClassNode();
        // no need to expand frames here as we only need the version and source file
        reader.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        final String sourceFile = cn.sourceFile;
        iBreakpoints = CFUtils.loadCfBreakpoints(sourceFile, cfm);
      } else {
        iBreakpoints = CFUtils.loadCfBreakpoints(location, cfm);
      }
      if (iBreakpoints.isEmpty()) {
        return null;
      }
      isCf = true;
    }
    // no breakpoints for this class, but we have a jsp breakpoints, and this is a jsp class
    else if (matchedTracepoints.isEmpty()
        && this.classPrefixTracepoints.containsKey(JSP_CLASS_KEY)
        && JSPUtils.isJspClass(this.jspSuffix, this.jspPackages,
        InstUtils.externalClassName(className))) {
      isCf = false;
      final SourceMap sourceMap = JSPUtils.getSourceMap(classfileBuffer);
      if (sourceMap == null) {
        LOGGER.debug("Cannot load source map for class: {}", className);
        return null;
      }

      final Collection<TracePointConfig> rawBreakpoints = JSPUtils.loadJSPTracepoints(sourceMap,
          this.classPrefixTracepoints.get(JSP_CLASS_KEY));
      if (rawBreakpoints.isEmpty()) {
        LOGGER.debug("Cannot load tracepoints for class: {}", className);
        return null;
      } else {
        iBreakpoints = new HashSet<>();
        for (TracePointConfig rawBreakpoint : rawBreakpoints) {
          final List<SourceMapLineStartEnd> mappedLines = sourceMap.map(
              InstUtils.fileName(rawBreakpoint.getPath()),
              rawBreakpoint.getLineNo());
          if (mappedLines.isEmpty()) {
            continue;
          }
          final int start = mappedLines.get(0).getStart();
          iBreakpoints.add(new JSPMappedBreakpoint(rawBreakpoint, start));
        }
      }
    }
    // else there is a tracepoint for this class
    else {
      isCf = false;
      iBreakpoints = matchedTracepoints;
    }
    LOGGER.debug("Transforming class: {}", className);

    try {
      if (reader == null) {
        reader = new ClassReader(classfileBuffer);
        cn = new ClassNode();
        // no need to expand frames here as we only need the version out
        reader.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
      }

      // if we are not java 1.1 and are greater than COMPUTE_ON_CLASS_VERSION (50 (java 1.6)) then compute frames
      final boolean classVersionNeedsComputeFrames = cn.version != org.objectweb.asm.Opcodes.V1_1
          && cn.version >= COMPUTE_ON_CLASS_VERSION;

      final ClassWriter writer = new ClassLoaderAwareClassWriter(reader,
          // compute the frames if we need to else just maxes as we are a class version that does not have frames
          classVersionNeedsComputeFrames ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS,
          loader);
      final Visitor visitor = new Visitor(writer, iBreakpoints, isCf);
      // if we are going to compute frames then we can skip them - as they will be ignored and re computed anyway.
      reader.accept(visitor,
          classVersionNeedsComputeFrames ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

      if (visitor.wasChanged()) {

        final byte[] res = writer.toByteArray();
        TransformerUtils.storeUnsafe(this.disPath, classfileBuffer, res, className);
        return res;
      } else {
        LOGGER.debug("Class {} not changed.", className);
        return null;
      }
    } catch (final SkipException s) {
      LOGGER.debug("transform skipped for {}", className);
    } catch (final Throwable t) {
      LOGGER.error("transform failed for {}", className, t);
    }
    return null;
  }

  private Collection<TracePointConfig> matchTracepoints(final String className,
      final String shortClassName) {
    final Map<String, TracePointConfig> classMatches = this.classPrefixTracepoints.get(className);
    if (classMatches != null) {
      return classMatches.values();
    }

    final Map<String, TracePointConfig> shortClassMatches = this.classPrefixTracepoints.get(
        shortClassName);
    if (shortClassMatches != null) {
      return shortClassMatches.values();
    }
    return Collections.emptyList();
  }

}
