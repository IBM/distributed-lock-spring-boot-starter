package com.ibm.distributedlock.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author seanyu
 */
@ConfigurationProperties(prefix = "distributed-lock")
@Data
public class DistributedReentrantLockProperties {

    private String lockType;

    private String enabled;

}
