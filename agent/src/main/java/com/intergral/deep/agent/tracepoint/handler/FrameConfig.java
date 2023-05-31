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

import com.intergral.deep.agent.types.TracePointConfig;

public class FrameConfig
{
    private final static int DEFAULT_MAX_VAR_DEPTH = 5;
    private final static int DEFAULT_MAX_VARIABLES = 1000;
    private final static int DEFAULT_MAX_COLLECTION_SIZE = 10;
    private final static int DEFAULT_MAX_STRING_LENGTH = 1024;
    private final static int DEFAULT_MAX_WATCH_VARS = 100;
    private final static int DEFAULT_MAX_TP_PROCESS_TIME = 100;

    private String frameType = null;
    private String stackType = null;
    private int maxVarDepth = -1;
    private int maxVariables = -1;
    private int maxCollectionSize = -1;
    private int maxStrLength = -1;
    private int maxWatchVars = -1;
    private int maxTpProcessTime = -1;

    public void process( final TracePointConfig tracePointConfig )
    {
        maxVarDepth = Math.max( tracePointConfig.getArg( "MAX_VAR_DEPTH", Integer.class, -1 ), maxVarDepth );
        maxVariables = Math.max( tracePointConfig.getArg( "MAX_VARIABLES", Integer.class, -1 ), maxVariables );
        maxCollectionSize = Math.max( tracePointConfig.getArg( "MAX_COLLECTION_SIZE", Integer.class, -1 ),
                maxCollectionSize );
        maxStrLength = Math.max( tracePointConfig.getArg( "MAX_STRING_LENGTH", Integer.class, -1 ), maxStrLength );
        maxWatchVars = Math.max( tracePointConfig.getArg( "MAX_WATCH_VARS", Integer.class, -1 ), maxWatchVars );
        maxTpProcessTime = Math.max( tracePointConfig.getArg( "MAX_TP_PROCESS_TIME", Integer.class, -1 ),
                maxTpProcessTime );

        final String frameType = tracePointConfig.getFrameType();
        if( frameType != null )
        {
            if( this.frameType == null )
            {
                this.frameType = frameType;
            }
            else if( TracePointConfig.frameTypeOrdinal( frameType ) >
                    TracePointConfig.frameTypeOrdinal( this.frameType ) )
            {
                this.frameType = frameType;
            }
        }

        final String stackType = tracePointConfig.getStackType();
        if( stackType != null )
        {
            if( this.stackType == null )
            {
                this.stackType = stackType;
            }
            else if( stackType.equals( TracePointConfig.STACK ) )
            {
                this.stackType = stackType;
            }
        }
    }

    public void close()
    {
        maxVarDepth = maxVarDepth == -1 ? DEFAULT_MAX_VAR_DEPTH : maxVarDepth;
        maxVariables = maxVariables == -1 ? DEFAULT_MAX_VARIABLES : maxVariables;
        maxCollectionSize = maxCollectionSize == -1 ? DEFAULT_MAX_COLLECTION_SIZE : maxCollectionSize;
        maxStrLength = maxStrLength == -1 ? DEFAULT_MAX_STRING_LENGTH : maxStrLength;
        maxWatchVars = maxWatchVars == -1 ? DEFAULT_MAX_WATCH_VARS : maxWatchVars;
        maxTpProcessTime = maxTpProcessTime == -1 ? DEFAULT_MAX_TP_PROCESS_TIME : maxTpProcessTime;

        frameType = frameType == null ? TracePointConfig.SINGLE_FRAME_TYPE : frameType;
        stackType = stackType == null ? TracePointConfig.STACK : stackType;
    }

    public boolean shouldCollectVars( final int currentFrameIndex )
    {
        if( this.frameType.equals( TracePointConfig.NO_FRAME_TYPE ) )
        {
            return false;
        }

        if( currentFrameIndex == 0 )
        {
            return true;
        }

        return this.frameType.equals( TracePointConfig.ALL_FRAME_TYPE );
    }

    public int maxVariables()
    {
        return this.maxVariables;
    }

    public int maxStringLength()
    {
        return this.maxStrLength;
    }

    public int maxDepth()
    {
        return this.maxVarDepth;
    }

    public int maxCollectionSize()
    {
        return this.maxCollectionSize;
    }
}
