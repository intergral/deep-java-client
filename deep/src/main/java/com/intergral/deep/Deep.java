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

package com.intergral.deep;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.hook.IDeepHook;
import com.intergral.deep.api.IDeepLoader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Deep {

  private static Deep DEEP_INSTANCE = null;
  private Object deepService = null;
  private Object reflection = null;


  public static void start() {
    Deep.config().start();
  }

  public static Deep getInstance() {
    if (DEEP_INSTANCE != null) {
      return DEEP_INSTANCE;
    } else {
      DEEP_INSTANCE = new Deep();
    }
    return DEEP_INSTANCE;
  }

  public static DeepConfigBuilder config() {
    return new DeepConfigBuilder();
  }

  public void startWithConfig(final String config) {
    getInstance().startDeep(config);
  }

  private void startDeep(final String config) {
    try {
      loadAgent(config);

      loadAPI();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void loadAgent(final String config) throws Throwable {
    final IDeepLoader loader = getLoader();
    final String pid = getPid();

    loader.load(pid, config);
  }


  /**
   * Get the process id for the current process
   *
   * @return the process id
   */
  String getPid() {
    String nameOfRunningVm = ManagementFactory.getRuntimeMXBean().getName();
    return nameOfRunningVm.substring(0, nameOfRunningVm.indexOf('@'));
  }


  /**
   * Load the loader for NerdVision
   *
   * @return the loader to use
   * @throws Throwable if we cannot load the loader
   */
  IDeepLoader getLoader() throws Throwable {
    final String property = System.getProperty("deep.loader", DeepLoader.class.getName());
    final String env = System.getenv("DEEP_LOADER");
    final String loader;
    if (env != null) {
      loader = env;
    } else {
      loader = property;
    }

    final Class<?> aClass = Class.forName(loader);
    final Constructor<?> constructor = aClass.getConstructor();
    final Object newInstance = constructor.newInstance();
    return (IDeepLoader) newInstance;
  }

  private void loadAPI() {
    if (this.deepService != null) {
      // api already loaded
      return;
    }

    try {
      final Class<?> aClass = Class.forName("com.intergral.deep.agent.AgentImpl");
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
   * Get an instance of the API to allow calling NerdVision directly
   *
   * @param <T> this should be {@link IDeep}
   * @return the new instance or {@link IDeep}
   * @throws IllegalStateException if NerdVision has not been started yet.
   */
  public <T> T api() {
    loadAPI();
    if (this.deepService == null) {
      throw new IllegalStateException("Must start Deep first!");
    }
    //noinspection unchecked
    return (T) deepService;
  }


  private void setProxyService(Object service) {
    final IDeepHook hook = (IDeepHook) service;
    this.deepService = hook.deepService();
    this.reflection = hook.reflectionService();
  }

  public <T> T reflection() {
    loadAPI();
    if (this.reflection == null) {
      throw new IllegalStateException("Must start Deep first!");
    }
    //noinspection unchecked
    return (T) reflection;
  }
}
