package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.TestLogger;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author yoav
 * @since 12/5/12
 */
public class ListsTests {

    @Rule
    public TestLogger testLogger = new TestLogger();

    @Rule
    public TestName name= new TestName();

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private int nTasks = 100000;
    private int readWriteFactor = 4;
    private AtomicInteger errors = new AtomicInteger(0);
    private List<Future<?>> futures = new ArrayList<>();
    private final Object lock = new Object();
    private AtomicLong writerTime = new AtomicLong();
    private AtomicInteger writerCount = new AtomicInteger();
    private AtomicLong readerIterateTime = new AtomicLong();
    private AtomicInteger readerIterateCount = new AtomicInteger();
    private AtomicLong readerRandomTime = new AtomicLong();
    private AtomicInteger readerRandomCount = new AtomicInteger();

    private static List<Stats> stats = new ArrayList<>();

    @AfterClass
    public static void printStats() {
        for (Stats stat: stats) {
            System.out.println(String.format("%40s (%s) - time: %,6d mSec, avgReaderIterateTime: %,9d nanoSec, avgReaderRandomTime: %,9d nanoSec, avgWriterTime: %,9d nanoSec", stat.methodName,
                    (stat.isOk?"ok   ":"error"), stat.methodTime, stat.avgReaderIterateTime, stat.avgReaderRandomTime, stat.avgWriterTime));
        }
    }

    @DoesNotWork
    @Test
    public void arrayList() {
        List<Integer> theList = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theList)));
            else
                futures.add(executorService.submit(reader(theList)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void linkedList() {
        List<Integer> theList = new LinkedList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theList)));
            else
                futures.add(executorService.submit(reader(theList)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void vector() {
        List<Integer> theList = new Vector<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theList)));
            else
                futures.add(executorService.submit(reader(theList)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @Test
    public void copyOnWriteList() {
        List<Integer> theList = new CopyOnWriteArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theList)));
            else
                futures.add(executorService.submit(reader(theList)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void synchronizedArrayList() {
        List<Integer> theList = Collections.synchronizedList(new ArrayList<Integer>());
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theList)));
            else
                futures.add(executorService.submit(reader(theList)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
        assertThat(errors.get(), is(0));
    }

    @Test
    public void synchronizedOnAllOperationsArrayList() {
        List<Integer> theList = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingSynchronized(writer(theList))));
            else
                futures.add(executorService.submit(lockUsingSynchronized(reader(theList))));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start));
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

    private Runnable writer(final List<Integer> theList) {
        return new Runnable() {
            @Override
            public void run() {
                long s = System.nanoTime();
                theList.add(ThreadLocalRandom.current().nextInt());
                writerTime.getAndAdd(System.nanoTime() - s);
                writerCount.incrementAndGet();
            }
        };
    }

    private Runnable reader(final List<Integer> theList) {
        return new Runnable() {
            @Override
            public void run() {

                long s = System.nanoTime();
                try {
                    for (int i=0; i < 100; i++)
                        theList.get(i % theList.size());
                }
                catch (Exception e) {
                    errors.incrementAndGet();
                    testLogger.log().debug("failure in sum - {}", e.getClass());
                }
                finally {
                    readerRandomTime.getAndAdd(System.nanoTime() - s);
                    readerRandomCount.getAndAdd(100);
                }

                s = System.nanoTime();
                try {
                    int total = 0;
                    for (Integer i: theList)
                        total += i;
                }
                catch (Exception e) {
                    errors.incrementAndGet();
                    testLogger.log().debug("failure in sum - {}", e.getClass());
                }
                finally {
                    readerIterateTime.getAndAdd(System.nanoTime() - s);
                    readerIterateCount.incrementAndGet();
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

    class Stats {
        long methodTime;
        long avgWriterTime;
        long avgReaderIterateTime;
        long avgReaderRandomTime;
        boolean isOk;
        String methodName;

        Stats(long methodTime) {
            this(methodTime, writerTime.get() / writerCount.get(), readerIterateTime.get() / readerIterateCount.get(),
                    readerRandomTime.get() / readerRandomCount.get(), errors.get() == 0);
        }

        Stats(long methodTime, long avgWriterTime, long avgReaderIterateTime, long avgReaderRandomTime, boolean isOk) {
            methodName = name.getMethodName();
            this.methodTime = methodTime;
            this.avgWriterTime = avgWriterTime;
            this.avgReaderIterateTime = avgReaderIterateTime;
            this.avgReaderRandomTime = avgReaderRandomTime;
            this.isOk = isOk;
        }
    }

}
