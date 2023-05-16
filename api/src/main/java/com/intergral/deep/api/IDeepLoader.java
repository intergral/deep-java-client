package com.intergral.deep.api;

import java.util.Map;

/**
 * This is how Deep is to be loaded, default provider is in the 'deep' module.
 */
public interface IDeepLoader
{
    /**
     * Load the Deep agent into the provided process id.
     *
     * @param pid    the current process id
     * @param config the config to use
     * @throws Throwable if loader fails
     */
    void load( final String pid, final Map<String, Object> config ) throws Throwable;
}
