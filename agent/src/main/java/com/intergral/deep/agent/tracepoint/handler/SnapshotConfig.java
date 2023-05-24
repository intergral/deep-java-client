package com.intergral.deep.agent.tracepoint.handler;

import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.types.TracePointConfig;

import java.util.Collection;

public class SnapshotConfig
{
    private final int maxVarDepth;
    private final int maxVariables;
    private final int maxCollectionSize;
    private final int maxStrLength;
    private final int maxWatchVars;
    private final int maxTpProcessTime;
    private final int maxProfileTime;
    private final int profileInterval;


    public SnapshotConfig( final Settings settings, final Collection<TracePointConfig> values )
    {
        final SnapshotConfig snapshotConfig = new SnapshotConfig( values );
        this.maxVarDepth = useIfSetElse( snapshotConfig.maxVarDepth,
                settings.getSettingAs( "default_max_depth", int.class ) );
        this.maxVariables = useIfSetElse( snapshotConfig.maxVariables,
                settings.getSettingAs( "default_max_vars", int.class ) );
        this.maxCollectionSize = useIfSetElse( snapshotConfig.maxCollectionSize,
                settings.getSettingAs( "default_max_list_len", int.class ) );
        this.maxStrLength = useIfSetElse( snapshotConfig.maxStrLength,
                settings.getSettingAs( "default_max_str_length", int.class ) );

        this.maxWatchVars = useIfSetElse( snapshotConfig.maxWatchVars,
                settings.getSettingAs( "default_max_watch_vars", int.class ) );
        this.maxTpProcessTime = useIfSetElse( snapshotConfig.maxTpProcessTime,
                settings.getSettingAs( "default_max_tp_process_time", int.class ) );
        this.maxProfileTime = useIfSetElse( snapshotConfig.maxProfileTime,
                settings.getSettingAs( "default_max_profile_time", int.class ) );
        this.profileInterval = useIfSetElse( snapshotConfig.profileInterval,
                settings.getSettingAs( "default_profile_interval", int.class ) );
    }


    public SnapshotConfig( final Collection<TracePointConfig> values )
    {
        int maxVarDepth = 0;
        int maxVariables = 0;
        int maxCollectionSize = 0;
        int maxStrLength = 0;

        int maxWatchVars = 0;
        int maxTpProcessTime = 0;
        int maxProfileTime = 0;
        int profileInterval = 0;
        for( final TracePointConfig value : values )
        {
            maxVarDepth = Math.max( value.getArg( "MAX_VAR_DEPTH", Integer.class, -1 ), maxVarDepth );
            maxVariables = Math.max( value.getArg( "MAX_VARIABLES", Integer.class, -1 ), maxVariables );
            maxCollectionSize = Math.max( value.getArg( "MAX_COLLECTION_SIZE", Integer.class, -1 ), maxCollectionSize );
            maxStrLength = Math.max( value.getArg( "MAX_STRING_LENGTH", Integer.class, -1 ), maxStrLength );

            maxWatchVars = Math.max( value.getArg( "MAX_WATCH_VARS", Integer.class, -1 ), maxWatchVars );
            maxTpProcessTime = Math.max( value.getArg( "MAX_TP_PROCESS_TIME", Integer.class, -1 ), maxTpProcessTime );
            maxProfileTime = Math.max( value.getArg( "MAX_PROFILE_TIME", Integer.class, -1 ), maxProfileTime );
            profileInterval = Math.max( value.getArg( "PROFILE_INTERVAL", Integer.class, -1 ), profileInterval );
        }
        this.maxVarDepth = maxVarDepth;
        this.maxVariables = maxVariables;
        this.maxCollectionSize = maxCollectionSize;
        this.maxStrLength = maxStrLength;

        this.maxWatchVars = maxWatchVars;
        this.maxTpProcessTime = maxTpProcessTime;
        this.maxProfileTime = maxProfileTime;
        this.profileInterval = profileInterval;
    }

    private int useIfSetElse( final int maxCollectionSize, final Integer defaultVal )
    {
        if( maxCollectionSize == 0 )
        {
            return defaultVal;
        }
        return maxCollectionSize;
    }

    public int getMaxVarDepth()
    {
        return maxVarDepth;
    }


    public int getMaxVariables()
    {
        return maxVariables;
    }


    public int getMaxCollectionSize()
    {
        return maxCollectionSize;
    }


    public int getMaxStrLength()
    {
        return maxStrLength;
    }


    public int getMaxWatchVars()
    {
        return maxWatchVars;
    }


    public int getMaxTpProcessTime()
    {
        return maxTpProcessTime;
    }


    public int getMaxProfileTime()
    {
        return maxProfileTime;
    }


    public int getProfileInterval()
    {
        return profileInterval;
    }
}
