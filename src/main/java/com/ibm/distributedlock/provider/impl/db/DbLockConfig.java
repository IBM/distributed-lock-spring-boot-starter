package com.ibm.distributedlock.provider.impl.db;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "distributed-lock", name = "lock-type", havingValue = "db")
@Configuration
@MapperScan(basePackages = "com.ibm.distributedlock.provider.impl.db")
public class DbLockConfig {
}
