package com.ibm.distributedlock.provider;

import java.util.concurrent.TimeUnit;

/**
 * @author seanyu
 */
public interface LockProvider {

    String getType();

    /**
     * init lock key-value
     * @param key
     * @param value
     * @return
     */
    boolean initVal(String key, String value);
    /**
     * del lock key-value
     * @param key
     * @param value
     * @return
     */
    boolean del(String key, String value);

    /**
     * init lock key-value with ttl
     * @param key
     * @param value
     * @param ttl
     * @param timeUnit
     * @return
     */
    boolean initValWithTtl(String key, String value, long ttl, TimeUnit timeUnit);

    /**
     * del lock key-value with ttl
     * @param key
     * @param value
     * @param ttl
     * @param timeUnit
     * @return
     */
    boolean extendTtl(String key, String value, long ttl, TimeUnit timeUnit);
}
