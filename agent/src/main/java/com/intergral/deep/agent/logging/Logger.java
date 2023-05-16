package com.intergral.deep.agent.logging;

import com.intergral.deep.agent.AgentImpl;
import com.intergral.deep.agent.settings.Settings;
import org.slf4j.LoggerFactory;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class Logger
{
    public static org.slf4j.Logger configureLogging( final Settings settings )
    {
        final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.nerdvision");
        logger.setUseParentHandlers(false);
        final ConsoleHandler handler = new ConsoleHandler();
        logger.addHandler(handler);

        final Level settingAs = settings.getSettingAs("logging.level", Level.class);
        handler.setLevel(settingAs);
        logger.setLevel(settingAs);
        return LoggerFactory.getLogger( AgentImpl.class);
    }
}
