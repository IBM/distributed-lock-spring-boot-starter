package com.ibm.distributedlock;

import com.ibm.distributedlock.provider.impl.redis.RedisLockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DistributedlockApplicationTests {

    @Autowired
    private RedisLockProvider redisLockProvider;

    @Test
    public void contextLoads() {

    }

    @Test
    public void testRedis() {
        System.out.println(redisLockProvider.initVal("test","haha"));
        System.out.println(redisLockProvider.initValWithTtl("test1","haha", 1, TimeUnit.MINUTES));
        System.out.println(redisLockProvider.extendTtl("test1","haha", 2, TimeUnit.MINUTES));
        System.out.println(redisLockProvider.del("test", "haha"));
        System.out.println(redisLockProvider.del("test1", "haha"));
    }

}
