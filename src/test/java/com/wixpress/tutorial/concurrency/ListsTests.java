package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.TestLogger;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author yoav
 * @since 12/5/12
 */
public class ListsTests {

    @Rule
    public TestLogger testLogger = new TestLogger();

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private int nTasks = 1000;
    private int readWriteFactor = 4;
    private AtomicInteger errors = new AtomicInteger(0);
    private List<Future<?>> futures = new ArrayList<>();
    private final Object lock = new Object();

    @DoesNotWork
    @Test
    public void arrayList() {
        List<Integer> theList = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(producer(theList)));
            else
                futures.add(executorService.submit(summer(theList)));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void linkedList() {
        List<Integer> theList = new LinkedList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(producer(theList)));
            else
                futures.add(executorService.submit(summer(theList)));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void vector() {
        List<Integer> theList = new Vector<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(producer(theList)));
            else
                futures.add(executorService.submit(summer(theList)));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @Test
    public void copyOnWriteList() {
        List<Integer> theList = new CopyOnWriteArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(producer(theList)));
            else
                futures.add(executorService.submit(summer(theList)));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void synchronizedArrayList() {
        List<Integer> theList = Collections.synchronizedList(new ArrayList<Integer>());
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(producer(theList)));
            else
                futures.add(executorService.submit(summer(theList)));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @Test
    public void synchronizedOnAllOperationsArrayList() {
        List<Integer> theList = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingSynchronized(producer(theList))));
            else
                futures.add(executorService.submit(lockUsingSynchronized(summer(theList))));
        }

        waitForAllToComplete();
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    private void waitForAllToComplete() {
        for (Future future: futures)
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
    }

    private Runnable producer(final List<Integer> theList) {
        return new Runnable() {
            @Override
            public void run() {
                theList.add(ThreadLocalRandom.current().nextInt());
            }
        };
    }

    private Runnable summer(final List<Integer> theList) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    int total = 0;
                    for (Integer i: theList)
                        total += i;
                }
                catch (Exception e) {
                    errors.incrementAndGet();
                    testLogger.log().debug("failure in sum - {}", e.getClass());
                }
            }
        };
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

}
