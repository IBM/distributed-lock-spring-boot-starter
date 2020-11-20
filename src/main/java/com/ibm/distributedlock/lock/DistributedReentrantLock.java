package com.ibm.distributedlock.lock;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibm.distributedlock.provider.LockProvider;
import com.ibm.distributedlock.utils.NetUtils;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * a distributed reentrant lock implements {@link Lock}
 * based on a inner class {@link Sync} extends {@link AbstractDistributedQueuedSynchronizer} which provides a non fair CLH lock
 * @author seanyu
 */
public class DistributedReentrantLock implements Lock {

    private final Sync sync;

    private String lockKey;

    private LockProvider lockProvider;

    public DistributedReentrantLock(String lockTarget,LockProvider lockProvider){
        this.lockKey = "LOCK" + "_" + lockTarget;
        this.lockProvider = lockProvider;
        sync = new Sync();
    }


    @Override
    public void lock() {
        sync.lock();
    }

    public void lockWithExpireTime(long ttl, TimeUnit timeUnit) {
        sync.lock(ttl, timeUnit);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public void lockInterruptiblyWithExpireTime(long ttl, TimeUnit timeUnit) throws InterruptedException {
        sync.acquireInterruptibly(1, ttl, timeUnit);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    public boolean tryLockWithExpireTime(long ttl, TimeUnit timeUnit) {
        return sync.tryAcquire(1, ttl, timeUnit);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return sync.tryAcquireNanos(1, timeUnit.toNanos(timeout));
    }

    public boolean tryLockWithExpireTime(long timeout, TimeUnit timeOutTimeUnit, long ttl, TimeUnit ttlTimeUnit) throws InterruptedException {
        return sync.tryAcquireNanos(1, timeOutTimeUnit.toNanos(timeout), ttl, ttlTimeUnit);
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }


    /**
     * inner class extends {@link AbstractDistributedQueuedSynchronizer}.
     * because this lock is a distributed version, threads on different servers will be blocked in different CLH queues,
     * this lock cannot be fair (thread FIFO).
     * we only provide a non fair version implementation.
     * also we only provide a exclusive mode lock at present.
     */
    class Sync extends AbstractDistributedQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        private ThreadLocal<Future> threadLocal = new ThreadLocal<>();

        Sync(){
        }

        public void startExtendTtlThread(String lockKey, String lockValue, long ttl, TimeUnit timeUnit){
            ThreadFactory ttlThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("extend-ttl-thread-%d")
                    .setDaemon(true)
                    .build();
            Future future = new ScheduledThreadPoolExecutor(1, ttlThreadFactory).scheduleWithFixedDelay(
                    () -> lockProvider.extendTtl(lockKey, lockValue, ttl, timeUnit),
                    (ttl/4)*3 - 1,
                    (ttl/4)*3,
                    timeUnit
            );
            threadLocal.set(future);
        }

        final String setLockValue(Thread current){
            return NetUtils.getLocalAddress() + "_" + current.toString() + "_" + current.getId();
        }

        /**
         * lock given key
         */
        final void lock() {
            String lockValue = setLockValue(Thread.currentThread());
            if (lockProvider.initVal(lockKey, lockValue)) {
                setExclusiveOwnerThread(Thread.currentThread());
                setState(1);
            }else {
                acquire(1);
            }
        }

        /**
         * lock given key with a time to live (expire time)
         * ttl must be greater than 1 second
         * @param ttl time to live (expire time), be greater than 1 second
         * @param timeUnit see {@link TimeUnit}
         */
        final void lock(long ttl, TimeUnit timeUnit) {
            String lockValue = setLockValue(Thread.currentThread());
            if (lockProvider.initValWithTtl(lockKey, lockValue, ttl, timeUnit)) {
                startExtendTtlThread(lockKey, lockValue, ttl, timeUnit);
                setExclusiveOwnerThread(Thread.currentThread());
                setState(1);
            }else {
                acquire(1, ttl, timeUnit);
            }
        }

        /**
         * a non fair tryLock.
         * both lock K-V is stored in lock provider
         * reentrant state is stored by CLH lock queue
         * @param acquires always be 1 because distributed lock only have a exclusive mode
         * @return true if acquire lock success
         */
        @Override
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            String lockValue = setLockValue(current);
            int c = getState();
            if (c == 0) {
                if (lockProvider.initVal(lockKey, lockValue)) {
                    setState(1);
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                // overflow
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * a non fair tryLock with ttl.
         * @param acquires always be 1 because distributed lock only have a exclusive mode
         * @param ttl time to live (expire time), be greater than 1 second
         * @param timeUnit see {@link TimeUnit}
         * @return
         */
        @Override
        protected final boolean tryAcquire(int acquires, long ttl, TimeUnit timeUnit) {
            final Thread current = Thread.currentThread();
            String lockValue = setLockValue(current);
            int c = getState();
            if (c == 0) {
                if (lockProvider.initValWithTtl(lockKey, lockValue, ttl, timeUnit)) {
                    startExtendTtlThread(lockKey, lockValue, ttl, timeUnit);
                    setState(1);
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                // overflow
                if (nextc < 0) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(nextc);
                return true;
            }
            return false;
        }

        /**
         * a non fair tryUnLock.
         * both lock K-V is stored in lock provider
         * reentrant state is stored by CLH lock queue
         * @param releases always be 1 because distributed lock only have a exclusive mode
         * @return true if release lock success
         */
        @Override
        protected final boolean tryRelease(int releases) {
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                throw new IllegalMonitorStateException();
            }
            int c = getState() - releases;
            boolean free = false;
            if (c == 0) {
                String lockValue = setLockValue(getExclusiveOwnerThread());
                for (;;){
                    if (lockProvider.del(lockKey, lockValue)) {
                        if (threadLocal.get() != null) {
                            // interrupt the running thread in thread pool
                            threadLocal.get().cancel(true);
                            threadLocal.remove();
                        }
                        break;
                    }
                }
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

    }

}
