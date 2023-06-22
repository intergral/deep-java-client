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

package com.intergral.deep.agent.logging;

import com.intergral.deep.agent.AgentImpl;
import com.intergral.deep.agent.settings.Settings;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import org.slf4j.LoggerFactory;

public class Logger {

  public static void configureLogging(final Settings settings) {
    final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.intergral");
    logger.setUseParentHandlers(false);
    final ConsoleHandler handler = new ConsoleHandler();
    logger.addHandler(handler);

    final Level loggingLevel = settings.getSettingAs("logging.level", Level.class);
    final String loggingPath = settings.getSettingAs("logging.path", String.class);

    if (loggingPath != null && !loggingPath.isEmpty()) {
      try {
        final FileHandler fileHandler = new FileHandler(loggingPath);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
      } catch (IOException e) {
        System.out.println("Cannot initialize file handler for logger.");
        System.err.println("Cannot initialize file handler for logger.");
      }
    }

    handler.setLevel(loggingLevel);
    logger.setLevel(loggingLevel);
    LoggerFactory.getLogger(AgentImpl.class);
  }
}
