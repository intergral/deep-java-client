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
import com.intergral.deep.agent.tracepoint.inst.jsp.SourceMap;
import com.intergral.deep.agent.tracepoint.inst.jsp.SourceMapLineStartEnd;
import com.intergral.deep.agent.types.TracePointConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class TracepointInstrumentationService implements ClassFileTransformer
{
    public final static long COMPUTE_ON_CLASS_VERSION = Long.getLong( "nv.compute.class.version", 50L );
    private static final Logger LOGGER = LoggerFactory.getLogger( TracepointInstrumentationService.class );

    private final Instrumentation inst;
    private final Settings settings;
    private final String disPath;
    private final String jspSuffix;
    private final List<String> jspPackages;

    private Map<String, Map<String, TracePointConfig>> classPrefixBreakpoints = new ConcurrentHashMap<>();


    public TracepointInstrumentationService( final Instrumentation inst, final Settings settings )
    {
        this.inst = inst;
        this.settings = settings;
        disPath = settings.getSettingAs( "transform.path", String.class );
        //noinspection unchecked
        this.jspPackages = settings.getSettingAs( "jsp.packages", List.class );
        this.jspSuffix = settings.getSettingAs( "jsp.suffix", String.class );
    }

    static Set<TracePointConfig> loadJspBreakpoints( final Class<?> loadedClass,
                                                     final Map<String, TracePointConfig> jsp )
    {
        return loadJspBreakpoints( JSPUtils.getSourceMap( loadedClass ), jsp );
    }

    static Set<TracePointConfig> loadJspBreakpoints( final SourceMap sourceMap,
                                                     final Map<String, TracePointConfig> jsp )
    {
        if( sourceMap == null )
        {
            return Collections.emptySet();
        }

        final Set<TracePointConfig> matchedJsp = new HashSet<>();
        final List<String> filenames = sourceMap.getFilenames();
        for( Map.Entry<String, TracePointConfig> entry : jsp.entrySet() )
        {
            final TracePointConfig value = entry.getValue();
            final String fileName = InstUtils.fileName( value.getPath() );
            if( filenames.contains( fileName ) )
            {
                matchedJsp.add( value );
            }
        }
        return matchedJsp;
    }

    static Set<TracePointConfig> loadCfBreakpoints( final URL location,
                                                    final Map<String, TracePointConfig> values )
    {
        final Set<TracePointConfig> iBreakpoints = new HashSet<>();
        final Collection<TracePointConfig> breakpoints = values.values();
        for( TracePointConfig breakpoint : breakpoints )
        {
            final String srcRoot = breakpoint.getArgs().get( "src_root" );
            final String relPathFromNv = breakpoint.getPath();
            final String locationString = location.toString();
            if( (srcRoot != null && locationString.endsWith( relPathFromNv.substring( srcRoot.length() ) ))
                    || locationString.endsWith( relPathFromNv )
                    || (relPathFromNv.startsWith( "/src/main/cfml" )
                    && locationString.endsWith( relPathFromNv.substring( "/src/main/cfml".length() ) ))
            )
            {
                iBreakpoints.add( breakpoint );
            }
        }
        return iBreakpoints;
    }

    static Set<TracePointConfig> loadCfBreakpoints( final String location,
                                                    final Map<String, TracePointConfig> values )
    {
        if( location == null )
        {
            return Collections.emptySet();
        }
        final Set<TracePointConfig> iBreakpoints = new HashSet<>();
        final Collection<TracePointConfig> breakpoints = values.values();
        for( TracePointConfig breakpoint : breakpoints )
        {
            final String relPathFromNv = breakpoint.getPath();
            // some versions of lucee use lowercase file names
            if( Utils.endsWithIgnoreCase( relPathFromNv, location ) )
            {
                iBreakpoints.add( breakpoint );
            }
        }
        return iBreakpoints;
    }

    public synchronized void processBreakpoints( final Collection<TracePointConfig> breakpointResponse )
    {
        final Map<String, Map<String, TracePointConfig>> existingBreakpoints = this.classPrefixBreakpoints;
        final Set<String> newBreakpointOnExistingClasses = new HashSet<>();
        final Map<String, Map<String, TracePointConfig>> newBreakpoints = new HashMap<>();

        // process new breakpoints mapping to new breakpoints map { className -> { breakpoint id -> breakpoint } }
        for( final TracePointConfig tracePointConfig : breakpointResponse )
        {
            final String fullClass = TracepointUtils.estimatedClassRoot( tracePointConfig );
            if( newBreakpoints.containsKey( fullClass ) )
            {
                newBreakpoints.get( fullClass ).put( tracePointConfig.getId(), tracePointConfig );
            }
            else
            {
                final HashMap<String, TracePointConfig> value = new HashMap<>();
                value.put( tracePointConfig.getId(), tracePointConfig );

                newBreakpoints.put( fullClass, value );
            }
            final Map<String, TracePointConfig> existingConfig = existingBreakpoints.get( fullClass );
            if( existingConfig != null && !existingConfig.containsKey( tracePointConfig.getId() ) )
            {
                newBreakpointOnExistingClasses.add( fullClass );
            }
        }
        this.classPrefixBreakpoints = new ConcurrentHashMap<>( newBreakpoints );

        // build class scanners
        final CompositeClassScanner compositeClassScanner = new CompositeClassScanner();

        // scanner to handle classes that no longer have classes and need transformed
        final IClassScanner removedTracepoints = reTransformClassesThatNoLongerHaveTracePoints(
                new HashSet<>( existingBreakpoints.keySet() ), new HashSet<>( newBreakpoints.keySet() ) );
        compositeClassScanner.addScanner( removedTracepoints );

        // scanner to handle classes that now have tracepoints and need transformed
        final IClassScanner newClasses = reTransformClassesThatAreNew( new HashSet<>( existingBreakpoints.keySet() ),
                new HashSet<>( newBreakpoints.keySet() ) );
        compositeClassScanner.addScanner( newClasses );

        // scanner to handle classes that have tracepoints already, but the configs have changed
        final SetClassScanner modifiedClasses = new SetClassScanner( newBreakpointOnExistingClasses );
        compositeClassScanner.addScanner( modifiedClasses );

        // scanner to handle JSP classes
        if( this.classPrefixBreakpoints.containsKey( "jsp" ) || existingBreakpoints.containsKey( "jsp" ) )
        {
            final Map<String, TracePointConfig> jsp = this.classPrefixBreakpoints.get( "jsp" );
            @SuppressWarnings("RedundantTypeArguments") final IClassScanner jspScanner = reTransFormJSPClasses( new HashMap<>(
                            jsp == null ? Collections.<String, TracePointConfig>emptyMap() : jsp ),
                    Utils.newMap( existingBreakpoints.get( "jsp" ) ) );
            compositeClassScanner.addScanner( jspScanner );
        }

        // scanner to handle CFM classes
        if( this.classPrefixBreakpoints.containsKey( "cfm" ) || existingBreakpoints.containsKey( "cfm" ) )
        {
            final Map<String, TracePointConfig> cfm = this.classPrefixBreakpoints.get( "cfm" );
            @SuppressWarnings("RedundantTypeArguments") final IClassScanner cfmScanner = reTransFormCfClasses( new HashMap<>(
                            cfm == null ? Collections.<String, TracePointConfig>emptyMap() : cfm ),
                    Utils.newMap( existingBreakpoints.get( "cfm" ) ) );
            compositeClassScanner.addScanner( cfmScanner );
        }
        LOGGER.debug( "New breakpoint config {}", this.classPrefixBreakpoints );

        try
        {
            // scan loaded classes and transform
            final Class<?>[] classes = compositeClassScanner.scanAll( inst );
            if( classes.length != 0 )
            {
                // TODO: 15.07.20 look at redefineClasses
                inst.retransformClasses( classes );
            }
        }
        catch( Throwable e )
        {
            LOGGER.error( "Error re-transforming class", e );
        }
    }

    private IClassScanner reTransFormJSPClasses( final Map<String, TracePointConfig> jsp,
                                                 final Map<String, TracePointConfig> oldJsp )
    {
        final Map<String, TracePointConfig> removedBreakpoints = withRemoved( jsp, oldJsp );
        return new JSPClassScanner( removedBreakpoints, this.jspSuffix, this.jspPackages );
    }

    Map<String, TracePointConfig> withRemoved( final Map<String, TracePointConfig> jsp,
                                               final Map<String, TracePointConfig> oldJsp )
    {
        final Set<String> newIds = jsp.keySet();
        final Set<String> oldIds = oldJsp.keySet();

        oldIds.removeAll( newIds );

        for( String oldId : oldIds )
        {
            jsp.put( oldId, oldJsp.get( oldId ) );
        }

        return jsp;
    }

    IClassScanner reTransFormCfClasses( final Map<String, TracePointConfig> cfm,
                                        final Map<String, TracePointConfig> oldCfm )
    {
        final Map<String, TracePointConfig> removedBreakpoints = withRemoved( cfm, oldCfm );

        return new CFClassScanner( removedBreakpoints );
    }

    public URL getLocation( final ProtectionDomain protectionDomain )
    {
        return protectionDomain.getCodeSource().getLocation();
    }

    private IClassScanner reTransformClassesThatAreNew( final Set<String> existingClasses,
                                                        final Set<String> newClasses )
    {
        newClasses.removeAll( existingClasses );
        return new SetClassScanner( newClasses );
    }


    private IClassScanner reTransformClassesThatNoLongerHaveTracePoints( final Set<String> existingClasses,
                                                                         final Set<String> newClasses )
    {
        existingClasses.removeAll( newClasses );
        return new SetClassScanner( existingClasses );
    }


    @Override
    public byte[] transform( final ClassLoader loader,
                             final String classNameP,
                             final Class<?> classBeingRedefined,
                             final ProtectionDomain protectionDomain,
                             final byte[] classfileBuffer )
    {
        final boolean isCf;
        ClassReader reader = null;
        ClassNode cn = null;
        final Collection<TracePointConfig> iBreakpoints;
        final String className = InstUtils.internalClassStripInner( classNameP );
        // no breakpoints for this class or any CF classes
        if( !this.classPrefixBreakpoints.containsKey( className ) && !this.classPrefixBreakpoints.containsKey( "cfm" )
                && !this.classPrefixBreakpoints.containsKey( "jsp" ) )
        {
            return null;
        }
        // no breakpoints for this class, but we have a cfm breakpoints, and this is a cfm class
        else if( !this.classPrefixBreakpoints.containsKey( className ) &&
                this.classPrefixBreakpoints.containsKey( "cfm" )
                && CFUtils.isCfClass( classNameP ) )
        {
            final Map<String, TracePointConfig> cfm = this.classPrefixBreakpoints.get( "cfm" );
            final URL location = getLocation( protectionDomain );
            if( location == null )
            {
                reader = new ClassReader( classfileBuffer );
                cn = new ClassNode();
                // no need to expand frames here as we only need the version and source file
                reader.accept( cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE );
                final String sourceFile = cn.sourceFile;
                iBreakpoints = loadCfBreakpoints( sourceFile, cfm );
            }
            else
            {
                iBreakpoints = loadCfBreakpoints( location, cfm );
            }
            if( iBreakpoints.isEmpty() )
            {
                return null;
            }
            isCf = true;
        }
        // no breakpoints for this class, but we have a jsp breakpoints, and this is a jsp class
        else if( !this.classPrefixBreakpoints.containsKey( className ) &&
                this.classPrefixBreakpoints.containsKey( "jsp" )
                && JSPUtils.isJspClass( this.jspSuffix, this.jspPackages, InstUtils.externalClassName( className ) ) )
        {
            isCf = false;
            final SourceMap sourceMap = JSPUtils.getSourceMap( classfileBuffer );
            if( sourceMap == null )
            {
                LOGGER.debug( "Cannot load source map for class: {}", className );
                return null;
            }

            final Collection<TracePointConfig> rawBreakpoints = loadJspBreakpoints( sourceMap,
                    this.classPrefixBreakpoints.get( "jsp" ) );
            if( rawBreakpoints.isEmpty() )
            {
                LOGGER.debug( "Cannot load tracepoints for class: {}", className );
                return null;
            }
            else
            {
                iBreakpoints = new HashSet<>();
                for( TracePointConfig rawBreakpoint : rawBreakpoints )
                {
                    final List<SourceMapLineStartEnd> mappedLines = sourceMap.map( InstUtils.fileName( rawBreakpoint.getPath() ),
                            rawBreakpoint.getLineNo() );
                    if( mappedLines.isEmpty() )
                    {
                        continue;
                    }
                    final int start = mappedLines.get( 0 ).getStart();
                    iBreakpoints.add( new JSPMappedBreakpoint( rawBreakpoint, start ) );
                }
            }
        }
        // else there is a tracepoint for this class
        else
        {
            isCf = false;
            iBreakpoints = this.classPrefixBreakpoints.get( className ).values();
        }
        LOGGER.debug( "Transforming class: {}", className );

        try
        {
            if( reader == null )
            {
                reader = new ClassReader( classfileBuffer );
                cn = new ClassNode();
                // no need to expand frames here as we only need the version out
                reader.accept( cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG );
            }

            // if we are not java 1.1 and are greater than COMPUTE_ON_CLASS_VERSION (50 (java 1.6)) then compute frames
            final boolean classVersionNeedsComputeFrames = (cn.version != org.objectweb.asm.Opcodes.V1_1
                    && cn.version >= COMPUTE_ON_CLASS_VERSION);

            final ClassWriter writer = new ClassLoaderAwareClassWriter( reader,
                    // compute the frames if we need to else just maxes as we are a class version that does not have frames
                    classVersionNeedsComputeFrames ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS,
                    loader );
            final Visitor visitor = new Visitor( writer, iBreakpoints, isCf );
            // if we are going to compute frames then we can skip them - as they will be ignored and re computed anyway.
            reader.accept( visitor,
                    classVersionNeedsComputeFrames ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES );

            if( visitor.wasChanged() )
            {

                final byte[] res = writer.toByteArray();
                TransformerUtils.storeUnsafe( this.disPath, classfileBuffer, res, className );
                return res;
            }
            else
            {
                LOGGER.debug( "Class {} not changed.", className );
                return null;
            }
        }
        catch( final SkipException s )
        {
            LOGGER.debug( "transform skipped for {}", className );
        }
        catch( final Throwable t )
        {
            LOGGER.error( "transform failed for {}", className, t );
        }
        return null;
    }
}
