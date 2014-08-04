package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.HasDeadLock;
import com.wixpress.tutorial.TestLogger;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.newSetFromMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author yoav
 * @since 12/5/12
 */
public class MapTests {

    @Rule
    public TestLogger testLogger = new TestLogger();
    @Rule
    public TestName name= new TestName();

    private ExecutorService executorService = Executors.newFixedThreadPool(100);
    private int nTasks = 100000;
    private int readWriteFactor = 4;
    private AtomicInteger readErrors = new AtomicInteger(0);
    private AtomicInteger writeErrors = new AtomicInteger(0);
    private List<Future<?>> futures = new ArrayList<>();
    private final Object lock = new Object();
    private AtomicLong writerTime = new AtomicLong();
    private AtomicInteger writerCount = new AtomicInteger();
    private AtomicLong readerTime = new AtomicLong();
    private AtomicInteger readerCount = new AtomicInteger();

    private static List<Stats> stats = new ArrayList<>();

    @AfterClass
    public static void printStats() {
        for (Stats stat: stats) {
            System.out.println(String.format("%50s (%s) - time: %,6d mSec, avgReaderTime: %,9d nanoSec, avgWriterTime: %,9d nanoSec", stat.methodName,
                    (stat.isOk?"ok   ":"error"), stat.methodTime, stat.avgConsumerTime, stat.avgProducesTime));
        }
    }

    @Ignore
    @HasDeadLock
    @DoesNotWork
    @Test
    public void hashSet() {
        Set<Integer> theSet = new HashSet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void linkedHashSet() {
        Set<Integer> theSet = new LinkedHashSet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void synchronizedHashSet() {
        Set<Integer> theSet = Collections.synchronizedSet(new HashSet<Integer>());
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @DoesNotWork
    @Test
    public void treeSet() {
        Set<Integer> theSet = new TreeSet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @Test
    public void setFromConcurrentMap() {
        Set<Integer> theSet = newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @Test
    public void ConcurrentSkipListSet() {
        Set<Integer> theSet = new ConcurrentSkipListSet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @Test
    public void CopyOnWriteArraySet() {
        Set<Integer> theSet = new CopyOnWriteArraySet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(writer(theSet)));
            else
                futures.add(executorService.submit(reader(theSet)));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    @Test
    public void synchronizedOnAllOperationsHashSet() {
        Set<Integer> theSet = new HashSet<>();
        long start = System.currentTimeMillis();
        for (int i=0; i < nTasks; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingSynchronized(writer(theSet))));
            else
                futures.add(executorService.submit(lockUsingSynchronized(reader(theSet))));
        }

        waitForAllToComplete();
        stats.add(new Stats(System.currentTimeMillis() - start, writerTime.get() / writerCount.get(), readerTime.get() / readerCount.get(), readErrors.get() + writeErrors.get() == 0));
        assertThat(readErrors.get(), is(0));
        assertThat(writeErrors.get(), is(0));
    }

    private void waitForAllToComplete() {
        for (Future future: futures)
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
    }

    private Runnable writer(final Set<Integer> theSet) {
        return new Runnable() {
            @Override
            public void run() {
                long s = System.nanoTime();
                try {
                    theSet.add(ThreadLocalRandom.current().nextInt());
                }
                catch (Exception e) {
                    writeErrors.incrementAndGet();
                    testLogger.log().debug("failure in writer - {}", e.getClass());
                }
                finally {
                    writerTime.getAndAdd(System.nanoTime() - s);
                    writerCount.incrementAndGet();
                }
            }
        };
    }

    private Runnable reader(final Set<Integer> theSet) {
        return new Runnable() {
            @Override
            public void run() {
                long s = System.nanoTime();
                try {
                    for (int i=0; i < 100; i++)
                        theSet.contains(i);
                }
                catch (Exception e) {
                    readErrors.incrementAndGet();
                    testLogger.log().debug("failure in sum - {}", e.getClass());
                }
                finally {
                    readerTime.getAndAdd(System.nanoTime() - s);
                    readerCount.incrementAndGet();
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
        long avgProducesTime;
        long avgConsumerTime;
        boolean isOk;
        String methodName;

        Stats(long methodTime, long avgProducesTime, long avgConsumerTime, boolean isOk) {
            methodName = name.getMethodName();
            this.methodTime = methodTime;
            this.avgProducesTime = avgProducesTime;
            this.avgConsumerTime = avgConsumerTime;
            this.isOk = isOk;
        }
    }
}
