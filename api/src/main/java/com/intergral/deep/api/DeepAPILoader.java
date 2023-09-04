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

package com.intergral.deep.api;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import java.lang.reflect.Method;

/**
 * This is a utility class that allows loading the Deep API.
 * <p>
 * We have this type separate from the deep module, so we can have custom loading in special uses of deep.
 */
public class DeepAPILoader {

  private static Object deepService = null;
  private static Object reflection = null;
  private DeepAPILoader() {
  }

  private static void loadAPI() {
    if (deepService != null) {
      // api already loaded
      return;
    }

    try {
      // we need to use the system class loader here, as when we run in OSGi or other complex environments. The class loader we have at
      // this point might be isolated and wont be able to load the agent class.
      final Class<?> aClass = ClassLoader.getSystemClassLoader().loadClass("com.intergral.deep.agent.AgentImpl");
      final Method registerBreakpointService = aClass.getDeclaredMethod("loadDeepAPI");
      final Object invoke = registerBreakpointService.invoke(null);
      if (invoke != null) {
        setProxyService(invoke);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * When we start Deep via the config, (ie not as a javaagent) we need to await the start. This should not take very long.
   * <p>
   * Essentially when we tell Bytebuddy to load the agent, this is sometimes done as an async external process. Which we cannot await. We
   * need to, however, get an instance of the deep api hooked back after deep has connected. So we just keep trying.
   * <p>
   * This should not ever fail, as the agent will either load, or we will get an exception from the load. Once it is loaded however it can
   * take some time to initialise depending on the size of the environment.
   */
  public static void awaitAPI() {
    if (deepService != null) {
      // api already loaded
      return;
    }
    while (deepService == null) {
      try {
        // this will throw a Deep not started error, if deep has not started.
        loadAPI();
      } catch (Exception ignored) {
        try {
          //noinspection BusyWait
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Get an instance of the API to allow calling NerdVision directly
   * <p>
   * This uses T as the type {@link IDeep} is not loaded so this class cannot use it.
   *
   * @param <T> this should be {@link IDeep}
   * @return the new instance or {@link IDeep}
   * @throws IllegalStateException if NerdVision has not been started yet.
   */
  public static <T> T api() {
    loadAPI();
    if (deepService == null) {
      throw new IllegalStateException("Must start Deep first!");
    }
    //noinspection unchecked
    return (T) deepService;
  }


  /**
   * Get an instance of the Reflection api used in deep.
   * <p>
   * This uses T as the type {@link com.intergral.deep.agent.api.reflection.IReflection} is not loaded so this class cannot use it.
   *
   * @param <T> this should be {@link com.intergral.deep.agent.api.reflection.IReflection}
   * @return the {@link com.intergral.deep.agent.api.reflection.IReflection} service
   */
  public static <T> T reflection() {
    loadAPI();
    if (reflection == null) {
      throw new IllegalStateException("Must start Deep first!");
    }
    //noinspection unchecked
    return (T) reflection;
  }

  private static void setProxyService(Object service) {
    final IDeepHook hook = (IDeepHook) service;
    deepService = hook.deepService();
    reflection = hook.reflectionService();
  }
}
