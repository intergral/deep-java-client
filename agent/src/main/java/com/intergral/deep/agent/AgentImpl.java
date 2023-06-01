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

package com.intergral.deep.agent;

import com.intergral.deep.agent.api.DeepVersion;
import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.logging.Logger;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import java.lang.instrument.Instrumentation;
import java.util.Map;

public class AgentImpl {

  public static void startup(final Instrumentation inst, final Map<String, String> args) {
    final Settings settings = Settings.build(args);
    Logger.configureLogging(settings);

    final TracepointInstrumentationService tracepointInstrumentationService =
        TracepointInstrumentationService.init(inst, settings);

    final DeepAgent deepAgent = new DeepAgent(settings, tracepointInstrumentationService);

    deepAgent.start();
  }

  public static Object loadDeepAPI() {
    return new IDeepHook() {

      @Override
      public IDeep deepService() {
        return new IDeep() {
          @Override
          public String getVersion() {
            return DeepVersion.VERSION;
          }
        };
      }

      @Override
      public IReflection reflectionService() {
        return ReflectionUtils.getReflection();
      }
    };
  }
}
