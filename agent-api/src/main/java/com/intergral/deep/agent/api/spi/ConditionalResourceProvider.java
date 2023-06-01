/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.api.spi;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;

/**
 * A resource provider that is only applied if the {@link #shouldApply(ISettings, Resource)} method
 * returns {@code true}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface ConditionalResourceProvider extends ResourceProvider {

  /**
   * If an implementation needs to apply only under certain conditions related to the config or the
   * existing state of the Resource being built, they can choose to override this default.
   *
   * @param settings The auto configuration properties
   * @param existing The current state of the Resource being created
   * @return false to skip over this ResourceProvider, or true to use it
   */
  boolean shouldApply(ISettings settings, Resource existing);
}
