package com.ibm.distributedlock;

import com.ibm.distributedlock.lock.DistributedReentrantLock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DistributedlockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class DistributedReentrantWithTtlLockTest {

    /**
     * DistributedReentrantLock instance
     */
    private DistributedReentrantLock lockOnServer1;

    /**
     * DistributedReentrantLock. Simulate as another server
     */
    private DistributedReentrantLock lockOnServer2;

    /**
     * DistributedReentrantLock instance
     */
    private DistributedReentrantLock lockOnServer3;

    /**
     * DistributedReentrantLock. Simulate as another server
     */
    private DistributedReentrantLock lockOnServer4;

    /**
     * DistributedReentrantLock. Simulate as another server
     */
    private DistributedReentrantLock lockOnServer5;

    /**
     * DistributedReentrantLock. Used for single server test
     */
    private DistributedReentrantLock lockSingleServer;

    /**
     * CountDownLatch used for multi servers
     */
    private static CountDownLatch cdLatch = new CountDownLatch(5);

    @Autowired
    private Function<String, DistributedReentrantLock> distributedReentrantLockFunction;

    @Before
    public void setup() {
        lockSingleServer = distributedReentrantLockFunction.apply("test");
        System.out.println(lockSingleServer);
        lockOnServer1 = distributedReentrantLockFunction.apply("test");
        System.out.println(lockOnServer1);
        lockOnServer2 = distributedReentrantLockFunction.apply("test");
        System.out.println(lockOnServer2);
        lockOnServer3 = distributedReentrantLockFunction.apply("test");
        System.out.println(lockOnServer3);
        lockOnServer4 = distributedReentrantLockFunction.apply("test");
        System.out.println(lockOnServer4);
        lockOnServer5 = distributedReentrantLockFunction.apply("test");
        System.out.println(lockOnServer5);
    }

    /**
     * Case1: AbstractQueuedSynchronizer for reentrant feature
     */
    @Test
    public void testReentrant() throws Exception {
        // Reentrant caller
        reentrantGateOne(lockSingleServer);

    }

    /**
     * Case2: AbstractQueuedSynchronizer for one server - multi threads
     */
    @Test
    public void testSingleServer() throws Exception {
        // Thread max work elapse is 2s, greater than the lease duration (1s)
        // It means when the thread hold the lock, lease must be expanded.
        launchSingleServer(1, "S1", lockSingleServer, 5000);

    }


    /**
     * Case3: AbstractQueuedSynchronizer for multi servers - multi threads
     */
    @Test
    public void testMultiServer() throws InterruptedException {

        try {
            // Launch servers "S1", "S2"
            new ServerThread(20, "S1", lockOnServer1, 6000).start();
            new ServerThread(20, "S2", lockOnServer2, 6000).start();
            new ServerThread(20, "S3", lockOnServer3, 6000).start();
            new ServerThread(20, "S4", lockOnServer4, 6000).start();
            new ServerThread(20, "S5", lockOnServer5, 6000).start();
            // Check
            cdLatch.await();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /**
     * Launch threads on a single server
     *
     * @param totalThread
     * @param serverName
     * @param lock
     * @param maxWorkElapsed
     */
    private static void launchSingleServer(int totalThread, String serverName, DistributedReentrantLock lock, int maxWorkElapsed) {
        List<Thread> threads = new ArrayList<>(totalThread);
        for (int i = 0; i < totalThread; i++) {
            String tName = serverName + "-t-" + i ;
            threads.add(i, new TestThread(tName, lock, maxWorkElapsed));
        }

        threads.forEach(Thread::start);
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("**** All Done **** " + serverName);
    }

    /**
     * Server thread
     */
    static class ServerThread extends Thread {
        int totalThread;
        String serverName;
        DistributedReentrantLock lock;
        int maxWorkElapsed;

        ServerThread(int totalThread, String serverName, DistributedReentrantLock lock, int maxWorkElapsed) {
            super(serverName);
            this.totalThread = totalThread;
            this.serverName = serverName;
            this.lock = lock;
            this.maxWorkElapsed = maxWorkElapsed;
            setDaemon(true);
        }

        @Override
        public void run() {
            launchSingleServer(totalThread, serverName, lock, maxWorkElapsed);
            cdLatch.countDown();
        }

    }

    /**
     * AbstractQueuedSynchronizer thread
     */
    static class TestThread extends Thread {

        private DistributedReentrantLock lock;
        private int maxWorkElapse;

        TestThread(String name, DistributedReentrantLock redisLock, int maxWorkElapse) {
            super(name);
            this.lock = redisLock;
            this.maxWorkElapse = maxWorkElapse;
            setDaemon(true);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start <= 60 * 1000L) {

                try {
                    long t = System.currentTimeMillis();
                    lock.lockWithExpireTime(5, TimeUnit.SECONDS);
                    System.out.println("*********************** Lock block ***********************");
                    System.out.println(getName() + " >>>>> get lock time:" + (System.currentTimeMillis() - t));

                    doWork();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(getName() + " >     Done!");
                    System.out.println();
                    lock.unlock();
                }

                break;
            }

        }

        private void doWork() throws InterruptedException {
//            int sleepTime = new Random().nextInt(maxWorkElapse);
            sleep(maxWorkElapse);
            System.out.println(getName() + " >>>   worked done for:" + maxWorkElapse);
        }
    }



    /**
     * Reentrant test tool methods
     */
    private void reentrantGateOne(DistributedReentrantLock lock) {
        try {
            System.out.println("method1 ready to lock.");
            lock.lockWithExpireTime(10, TimeUnit.SECONDS);
            System.out.println("method1 lock success. ++");

            reentrantGateTwo(lock);
            Thread.sleep(1000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("method1 ready to release lock.");
            lock.unlock();
            System.out.println("method1 unlocked success. --");
        }
    }

    private void reentrantGateTwo(DistributedReentrantLock lock) {
        try {
            System.out.println(">>>>method2 ready to lock.");
            lock.lockWithExpireTime(10, TimeUnit.SECONDS);
            System.out.println(">>>>method2 lock success. ++");

            Thread.sleep(1500);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(">>>>method2 ready to release lock.");
            lock.unlock();
            System.out.println(">>>>method2 unlocked success. --");
        }
    }
}
