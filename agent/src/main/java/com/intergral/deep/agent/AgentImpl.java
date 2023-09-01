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

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.agent.api.reflection.IReflection;
import com.intergral.deep.agent.logging.Logger;
import com.intergral.deep.agent.settings.Settings;
import com.intergral.deep.agent.tracepoint.inst.TracepointInstrumentationService;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This type is called from the {@link Agent} via reflection to load the agent after the jar we are in has been attached to the class path.
 */
public class AgentImpl {

  private AgentImpl() {
  }

  private static final CountDownLatch LATCH = new CountDownLatch(1);
  private static DeepAgent deepAgent;

  /**
   * Start the deep agent.
   *
   * @param inst the instrumentation object
   * @param args the agent arguments
   */
  public static void startup(final Instrumentation inst, final Map<String, String> args) {
    final Settings settings = Settings.build(args);
    Logger.configureLogging(settings);

    final TracepointInstrumentationService tracepointInstrumentationService =
        TracepointInstrumentationService.init(inst, settings);

    final DeepAgent deep = new DeepAgent(settings, tracepointInstrumentationService);

    deep.start();

    deepAgent = deep;
    LATCH.countDown();
  }

  /**
   * await the load of the dep api.
   *
   * @return the loaded api object
   * @throws InterruptedException if interupted
   */
  // called via reflection
  public static Object awaitLoadAPI() throws InterruptedException {
    LATCH.await();
    return loadDeepAPI();
  }

  /**
   * Load the deep API to be used outside the agent.
   * <p>
   * This method is defined as returning {@link Object} to not cause unwanted class loading of the type {@link IDeepHook}.
   *
   * @return a new instance of {@link IDeepHook}
   */
  public static Object loadDeepAPI() {
    if (deepAgent == null) {
      throw new IllegalStateException("Must start DEEP first");
    }
    return new IDeepHook() {

      @Override
      public IDeep deepService() {
        return deepAgent;
      }

      @Override
      public IReflection reflectionService() {
        return ReflectionUtils.getReflection();
      }
    };
  }
}
