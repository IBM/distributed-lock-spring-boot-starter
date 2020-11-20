package com.ibm.distributedlock.provider.impl.webspherecache;

import com.ibm.distributedlock.provider.LockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author seanyu
 */
@ConditionalOnProperty(prefix = "distributed-lock", name = "lock-type", havingValue = "redis")
@Component
public class WebSphereCacheLockProvider implements LockProvider {


    @Override
    public String getType() {
        return "websphere_cache";
    }

    @Override
    public boolean initVal(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean del(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean initValWithTtl(String key, String value, long ttl, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean extendTtl(String key, String value, long ttl, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }
}
