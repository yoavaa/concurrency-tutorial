package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.TestLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/5/12
 */
public class LockingTest {
    @Rule
    public TestLogger testLogger = new TestLogger();

    private Integer count1 = 0;
    private Integer count2 = 0;
    private AtomicInteger missMatches = new AtomicInteger(0);
    private List<Future<?>> futures = new ArrayList<>();
    private long start;
    private final int nCycles = 1000;
    private final int readWriteFactor = 10;

    private final static Object lock = new Object();
    private final static ReentrantLock reentrantLock = new ReentrantLock();
    private final static ReentrantLock fairReentrantLock = new ReentrantLock(true);
    private final static ReentrantReadWriteLock fairReadWriteLock = new ReentrantReadWriteLock(true);
    private final static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void logTime() {
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
    }

    @Test
    public void withoutLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(new Writer()));
            else
                futures.add(executorService.submit(new Reader()));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingSynchronized(new Writer())));
            else
                futures.add(executorService.submit(lockUsingSynchronized(new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withReentrantLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingReentrantLock(false, new Writer())));
            else
                futures.add(executorService.submit(lockUsingReentrantLock(false, new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withFairReentrantLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingReentrantLock(true, new Writer())));
            else
                futures.add(executorService.submit(lockUsingReentrantLock(true, new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withReadWriteLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(readWriteLock(false, false, new Writer())));
            else
                futures.add(executorService.submit(readWriteLock(false, true, new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withFairReadWriteLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(readWriteLock(true, false, new Writer())));
            else
                futures.add(executorService.submit(readWriteLock(true, true, new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }


    private void waitForAllTasksToComplete() {
        for (Future<?> future: futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                testLogger.log().debug("interrupt exception");
            } catch (ExecutionException e) {
                testLogger.log().debug("execution exception caused by {}", e.getCause());
            }
        }
    }


    private Runnable lockUsingSynchronized(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    runnable.run();
                }
            }
        };
    }

    private Runnable lockUsingReentrantLock(final boolean fair, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {

                getLock().lock();
                try {
                    runnable.run();
                }
                finally {
                    getLock().unlock();
                }
            }

            private ReentrantLock getLock() {
                if (fair)
                    return fairReentrantLock;
                else
                    return reentrantLock;
            }
        };
    }

    private Runnable readWriteLock(final boolean fair, final boolean readLock, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                if (readLock)
                    getLock().readLock().lock();
                else
                    getLock().writeLock().lock();
                try {
                    runnable.run();
                }
                finally {
                    if (readLock)
                        getLock().readLock().unlock();
                    else
                        getLock().writeLock().unlock();
                }
            }

            private ReentrantReadWriteLock getLock() {
                if (fair)
                    return fairReadWriteLock;
                else
                    return readWriteLock;
            }
        };
    }

    private class Writer implements Runnable {
        @Override
        public void run() {
            count1 += 1;
            try {
                Thread.sleep(0, 100000);
            } catch (InterruptedException e) {
                testLogger.log().debug("interrupted");
            }
            count2 += 1;
        }
    }

    private class Reader implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(0, 100000);
            } catch (InterruptedException e) {
                testLogger.log().debug("interrupted");
            }
            if (!count1.equals(count2))
                missMatches.incrementAndGet();
        }
    }

}
