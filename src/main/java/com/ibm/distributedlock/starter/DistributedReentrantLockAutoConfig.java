package com.ibm.distributedlock.starter;

import com.ibm.distributedlock.lock.DistributedReentrantLock;
import com.ibm.distributedlock.provider.LockProvider;
import com.ibm.distributedlock.provider.factory.LockProviderFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import java.util.function.Function;

/**
 * @author seanyu
 */
@EnableConfigurationProperties(DistributedReentrantLockProperties.class)
@ConditionalOnClass(DistributedReentrantLock.class)
@ConditionalOnProperty(prefix = "distributed-lock",value = "enabled", havingValue = "true")
@Configuration
@ComponentScan(basePackages="com.ibm.distributedlock")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DistributedReentrantLockAutoConfig {

    private final @NonNull
    DistributedReentrantLockProperties distributedReentrantLockProperties;

    private final @NonNull
    LockProviderFactory lockProviderFactory;

    @Bean
    public Function<String, DistributedReentrantLock> distributedReentrantLockFunction() {
        return this::distributedReentrantLock;
    }

    @Bean
    @Scope("prototype")
    @Lazy
    @ConditionalOnMissingBean(DistributedReentrantLock.class)
    public DistributedReentrantLock distributedReentrantLock(String lockTarget){
        String lockType = distributedReentrantLockProperties.getLockType();
        LockProvider lockProvider = lockProviderFactory.getLockProvider(lockType);
        return new DistributedReentrantLock(lockTarget,lockProvider);
    }

}
