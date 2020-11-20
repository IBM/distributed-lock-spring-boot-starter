package com.ibm.distributedlock.provider.impl.db;

import com.ibm.distributedlock.provider.LockProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author seanyu
 */
@ConditionalOnProperty(prefix = "distributed-lock", name = "lock-type", havingValue = "db")
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DbLockProvider implements LockProvider {

    private final @NonNull
    DbMapper dbMapper;

    @Override
    public String getType() {
        return "db";
    }

    @Override
    public boolean initVal(String key, String value) {
        try{
            return dbMapper.insert(key, value) != 0;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public boolean del(String key, String value) {
        try{
            return dbMapper.deleteByKeyAndValue(key, value) != 0;
        }catch (Exception e){
            return false;
        }
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
