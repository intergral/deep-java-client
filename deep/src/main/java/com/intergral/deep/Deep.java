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

/**
 * This is the main entry point to deep when using the API. All calls to deep should precede with a call to this class.
 * <p>
 * To start deep first configure the instance.
 * <code>
 * Deep.config().start();
 * </code>
 * <p>
 * To use a deep API use
 * <code>
 * Deep.getInstance().&lt;IDeep&gt;api()
 * </code>
 */
public class Deep {

  private static Deep DEEP_INSTANCE = null;
  private Object deepService = null;
  private Object reflection = null;


  /**
   * This is a shortcut for {@code Deep.config().start()}
   */
  public static void start() {
    Deep.config().start();
  }

  /**
   * This will create an instance of DEEP allowing access to the APIs from inside deep agent.
   *
   * @return {@link Deep}
   */
  public static Deep getInstance() {
    if (DEEP_INSTANCE != null) {
      return DEEP_INSTANCE;
    } else {
      DEEP_INSTANCE = new Deep();
    }
    return DEEP_INSTANCE;
  }

  /**
   * This is the main point to start with deep. Call this to create a config builder and to customise the config of deep before starting
   * it.
   *
   * @return {@link DeepConfigBuilder}
   */
  public static DeepConfigBuilder config() {
    return new DeepConfigBuilder();
  }

  /**
   * This allows deep to be started with the parsed config
   *
   * @param config as a string
   */
  void startWithConfig(final String config, final String jarPath) {
    getInstance().startDeep(config, jarPath);
  }

  /**
   * This allows deep to be started with the parsed config builder
   *
   * @param builder the config to use
   */
  public void start(final DeepConfigBuilder builder) {
    builder.start();
  }

  private void startDeep(final String config, final String jarPath) {
    try {
      loadAgent(config, jarPath);

      loadAPI();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  private void loadAgent(final String config, final String jarPath) throws Throwable {
    final IDeepLoader loader = getLoader();
    final String pid = getPid();

    loader.load(pid, config, jarPath);
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
   * <p>
   * This uses T as the type {@link IDeep} is not loaded so this class cannot use it.
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


  /**
   * Get an instance of the Reflection api used in deep.
   * <p>
   * This uses T as the type {@link com.intergral.deep.agent.api.reflection.IReflection} is not loaded so this class cannot use it.
   *
   * @return the {@link com.intergral.deep.agent.api.reflection.IReflection} service
   * @param <T> this should be {@link com.intergral.deep.agent.api.reflection.IReflection}
   */
  public <T> T reflection() {
    loadAPI();
    if (this.reflection == null) {
      throw new IllegalStateException("Must start Deep first!");
    }
    //noinspection unchecked
    return (T) reflection;
  }

  private void setProxyService(Object service) {
    final IDeepHook hook = (IDeepHook) service;
    this.deepService = hook.deepService();
    this.reflection = hook.reflectionService();
  }
}
