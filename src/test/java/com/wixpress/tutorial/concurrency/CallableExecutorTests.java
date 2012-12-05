package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.TestLogger;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wixpress.tutorial.concurrency.AsyncMatchers.eventually;
import static com.wixpress.tutorial.concurrency.AsyncMatchers.overTime;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/5/12
 */
public class CallableExecutorTests {

    private AtomicInteger count = new AtomicInteger(0);
    private final int nCycles = 1000;
    private int concurrentWorkingTasks = 0;
    private int maxConcurrentWorkingTasks = 0;

    private List<Future<Integer>> futures = new ArrayList<>();

    @Rule
    public TestLogger testLogger = new TestLogger();

    /**
     * this scenario has 10 worker threads and unlimited queue
     */
    @Test
    public void fixedExecutor() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i < nCycles; i++)
            futures.add(executorService.submit(task()));

        for (Future<Integer> future: futures)
            future.get();

        assertThat(overTime(new AsyncMatchers.Sampler<Integer>() {
            @Override
            public Integer Sample() {
                return count.get();
            }
        }, 10, 1000), eventually(is(nCycles)));

        executorService.shutdown();
        logMaxConcurrentWorkingTasks();
    }

    /**
     * this scenario will create as many worker threads as needed to complete the job
     * the queue is a fake - jobs start as soon as a new thread can be created
     */
    @Test
    public void cachedExecutor() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i=0; i < nCycles; i++)
            futures.add(executorService.submit(task()));

        for (Future<Integer> future: futures)
            future.get();

        assertThat(overTime(new AsyncMatchers.Sampler<Integer>() {
            @Override
            public Integer Sample() {
                return count.get();
            }
        }, 10, 1000), eventually(is(nCycles)));

        executorService.shutdown();
        logMaxConcurrentWorkingTasks();
    }

    /**
     * this scenario has 10 worker threads and 50 places in the queue for tasks.
     * task submission may fail is the queue is full
     */
    @DoesNotWork
    @Test
    public void fixedWithLimitedQueue() throws ExecutionException, InterruptedException {
        ExecutorService executorService = new ThreadPoolExecutor(10, 10,
                                              0L, TimeUnit.MILLISECONDS,
                                              new LinkedBlockingQueue<Runnable>(50));
        for (int i=0; i < nCycles; i++)
            try {
                futures.add(executorService.submit(task()));
            }
            catch (RejectedExecutionException e) {
                testLogger.log().debug("rejected task: {}", e.getClass());
            }

        for (Future<Integer> future: futures)
            future.get();

        assertThat(overTime(new AsyncMatchers.Sampler<Integer>() {
            @Override
            public Integer Sample() {
                return count.get();
            }
        }, 10, 1000), eventually(is(nCycles)));

        executorService.shutdown();
        logMaxConcurrentWorkingTasks();
    }

    private Callable<Integer> task() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                taskStarts();
                spendSomeTime();
                int cnt = count.incrementAndGet();
                taskCompletes();
                return cnt;
            }
        };
    }

    private void spendSomeTime() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void taskStarts() {
        synchronized(this) {
            concurrentWorkingTasks += 1;
            maxConcurrentWorkingTasks = Math.max(maxConcurrentWorkingTasks, concurrentWorkingTasks);
        }
    }

    private void taskCompletes() {
        synchronized (this) {
            concurrentWorkingTasks -= 1;
        }
    }

    private void logMaxConcurrentWorkingTasks() {
        testLogger.log().debug("max concurrent working tasks {}", maxConcurrentWorkingTasks);
    }
}
