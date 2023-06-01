/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intergral.deep.agent.api.spi;

import com.intergral.deep.agent.api.resource.Resource;
import com.intergral.deep.agent.api.settings.ISettings;

/**
 * A service provider interface (SPI) for providing a {@link Resource} that is merged into the
 * default resource.
 */
public interface ResourceProvider extends Ordered {

  Resource createResource(ISettings settings);
}
