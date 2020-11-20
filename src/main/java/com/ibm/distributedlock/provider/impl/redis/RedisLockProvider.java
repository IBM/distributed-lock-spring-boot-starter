package com.ibm.distributedlock.provider.impl.redis;

import com.ibm.distributedlock.provider.LockProvider;
import io.lettuce.core.RedisCommandInterruptedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


/**
 * @author seanyu
 */
@ConditionalOnProperty(prefix = "distributed-lock", name = "lock-type", havingValue = "redis")
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisLockProvider implements LockProvider {

    private final @NonNull StringRedisTemplate lockRedisTemplate;


    @Override
    public String getType() {
        return "redis";
    }

    @Override
    public boolean initVal(String key, String value) {

        try {
            return lockRedisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean del(String key, String value) {
        try {
            String del = "if (redis.call('get', KEYS[1]) == ARGV[1]) "
                    + " then "
                    + " redis.call('del', KEYS[1]) "
                    + " return true; "
                    + " else "
                    + " return false; "
                    + " end; ";
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(del);
            redisScript.setResultType(Boolean.class);
            return lockRedisTemplate.execute(redisScript, Collections.singletonList(key), value);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public boolean initValWithTtl(String key, String value, long ttl, TimeUnit timeUnit) {
        try {
            return lockRedisTemplate.execute(
                    (RedisCallback<Boolean>) connection -> (
                            (StringRedisConnection) connection).set(key, value, Expiration.from(ttl, timeUnit), RedisStringCommands.SetOption.SET_IF_ABSENT
                    )
            );
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean extendTtl(String key, String value, long ttl, TimeUnit timeUnit){
        try {
            String extendTtl = "if (redis.call('get', KEYS[1]) == ARGV[1]) "
                    + " then "
                    + " redis.call('pexpire', KEYS[1], ARGV[2]); "
                    + " return true;"
                    + " else "
                    + " return false; "
                    + " end;";
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>(extendTtl);
            redisScript.setResultType(Boolean.class);
            return lockRedisTemplate.execute(redisScript, Collections.singletonList(key), value, String.valueOf(timeUnit.toMillis(ttl)));
        } catch (Exception e){
            if (!(e.getCause().getCause() instanceof InterruptedException)){
                e.printStackTrace();
            }
            return false;
        }
    }
}
