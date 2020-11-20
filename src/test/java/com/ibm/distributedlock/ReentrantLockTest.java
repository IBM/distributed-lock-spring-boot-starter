package com.ibm.distributedlock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {


    /**
     * DistributedReentrantLock instance
     */
    private Lock lockOnServer1;

    /**
     * DistributedReentrantLock. Simulate as another server
     */
    private Lock lockOnServer2;

    /**
     * DistributedReentrantLock. Used for single server test
     */
    private Lock lockSingleServer;

    /**
     * CountDownLatch used for multi servers
     */
    private static CountDownLatch cdLatch = new CountDownLatch(2);

    @Before
    public void setup() {
        // non distributed lock
        lockSingleServer = new ReentrantLock();
        lockOnServer1 = new ReentrantLock();
        lockOnServer2 = new ReentrantLock();
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
            new DistributedReentrantLockTest.ServerThread(50, "S1", lockOnServer1, 5000).start();
            new DistributedReentrantLockTest.ServerThread(50, "S2", lockOnServer2, 5000).start();

            // Check
            cdLatch.await();

        } catch (Exception e) {
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
    private static void launchSingleServer(int totalThread, String serverName, Lock lock, int maxWorkElapsed) {
        List<Thread> threads = new ArrayList<>(totalThread);
        for (int i = 0; i < totalThread; i++) {
            final Thread current = Thread.currentThread();
            String tName = serverName + "-t-" + i;
            threads.add(i, new DistributedReentrantLockTest.TestThread(tName, lock, maxWorkElapsed));
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
        Lock lock;
        int maxWorkElapsed;

        ServerThread(int totalThread, String serverName, Lock lock, int maxWorkElapsed) {
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

        private Lock lock;
        private int maxWorkElapse;

        TestThread(String name, Lock redisLock, int maxWorkElapse) {
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
                    lock.lock();
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
            int sleepTime = new Random().nextInt(maxWorkElapse);
            sleep(sleepTime);
            System.out.println(getName() + " >>>   worked done for:" + sleepTime);
        }
    }

    /**
     * Reentrant test tool methods
     */
    private void reentrantGateOne(Lock lock) {
        try {
            System.out.println("method1 ready to lock.");
            lock.lock();
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

    private void reentrantGateTwo(Lock lock) {
        try {
            System.out.println(">>>>method2 ready to lock.");
            lock.lock();
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
