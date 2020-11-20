package com.ibm.distributedlock.provider.factory;

import com.ibm.distributedlock.provider.LockProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LockProviderFactory based on annotation,
 *
 * @author seanyu
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockProviderFactory {

    private final @NonNull
    List<LockProvider> lockProviders;

    private LockProvider lockProvider;

    public LockProvider getLockProvider(String lockType) {
        lockProviders.stream().filter(provider -> provider.getType().compareToIgnoreCase(lockType) == 0).findFirst().ifPresent(provider -> lockProvider = provider);
        return lockProvider;
    }

}
