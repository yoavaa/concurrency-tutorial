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

    private final static Object lock = new Object();
    private final static ReentrantLock reentrantLock = new ReentrantLock();
    private final static ReentrantLock fairReentrantLock = new ReentrantLock(true);

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
            if (i % 4 == 0)
                futures.add(executorService.submit(new Writer()));
            else
                futures.add(executorService.submit(new Reader()));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
    }

    @Test
    public void withLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % 4 == 0)
                futures.add(executorService.submit(lockUsingSynchronized(new Writer())));
            else
                futures.add(executorService.submit(lockUsingSynchronized(new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
    }

    @Test
    public void withReentrantLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % 4 == 0)
                futures.add(executorService.submit(lockUsingReentrantLock(new Writer())));
            else
                futures.add(executorService.submit(lockUsingReentrantLock(new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
    }

    @Test
    public void withFairReentrantLocking() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++) {
            if (i % 4 == 0)
                futures.add(executorService.submit(lockUsingFairReentrantLock(new Writer())));
            else
                futures.add(executorService.submit(lockUsingFairReentrantLock(new Reader())));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
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

    private Runnable lockUsingReentrantLock(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                reentrantLock.lock();
                try {
                    runnable.run();
                }
                finally {
                    reentrantLock.unlock();
                }
            }
        };
    }

    private Runnable lockUsingFairReentrantLock(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                fairReentrantLock.lock();
                try {
                    runnable.run();
                }
                finally {
                    fairReentrantLock.unlock();
                }
            }
        };
    }

    private class Writer implements Runnable {
        @Override
        public void run() {
            count1 += 1;
            try {
                Thread.sleep(0, 1000);
            } catch (InterruptedException e) {
                testLogger.log().debug("interrupted");
            }
            count2 += 1;
        }
    }

    private class Reader implements Runnable {
        @Override
        public void run() {
            if (!count1.equals(count2))
                missMatches.incrementAndGet();
        }
    }

}
