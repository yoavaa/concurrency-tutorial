package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wixpress.tutorial.concurrency.AsyncMatchers.eventually;
import static com.wixpress.tutorial.concurrency.AsyncMatchers.overTime;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/4/12
 */
public class CountingTest {

    private int counter = 0;
    private final Object lock = new Object();
    private volatile int volatileCounter = 0;
    private AtomicInteger atomicCounter = new AtomicInteger(0);
    private int nThreads = 500;
    private int nCycles = 1000;

    @DoesNotWork
    @Test
    public void run_100_threads() throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        counter++;
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();

        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads", System.nanoTime() - start, counter, nThreads));
        assertThat(counter, is(nThreads * nCycles));
    }

    @Test
    public void run_100_threads_with_lock() throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        synchronized (lock) {
                            counter++;
                        }
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();

        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads with lock", System.nanoTime() - start, counter,nThreads));
        assertThat(counter, is(nThreads * nCycles));
    }

    @DoesNotWork
    @Test
    public void run_100_threads_with_volatile() throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        volatileCounter++;
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();

        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads with volatile", System.nanoTime() - start, volatileCounter, nThreads));
        assertThat(volatileCounter, is(nThreads * nCycles));
    }

    @Test
    public void run_100_threads_with_atomic() throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        atomicCounter.incrementAndGet();
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();

        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads with atomic", System.nanoTime() - start, atomicCounter.get(), nThreads));
        assertThat(atomicCounter.get(), is(nThreads * nCycles));
    }

    @Test
    public void run_100_threads_with_single_worker() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                counter++;
                            }
                        });
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();



        assertThat(overTime(new AsyncMatchers.Sampler<Integer>() {
            @Override
            public Integer Sample() {
                return counter;
            }
        }, 10, 1000), eventually(is(nThreads * nCycles)));
        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads with actor", System.nanoTime() - start, counter, nThreads));

        executorService.shutdown();
    }

    @DoesNotWork
    @Test
    public void run_100_threads_with_multiple_actor() throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i=0; i < nThreads; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j=0; j < nCycles; j++)
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                counter++;
                            }
                        });
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread t: threads)
            t.join();



        assertThat(overTime(new AsyncMatchers.Sampler<Integer>() {
            @Override
            public Integer Sample() {
                return counter;
            }
        }, 10, 1000), eventually(is(nThreads * nCycles)));
        System.out.println(String.format("%,d nanoSec counter: %d - run %d threads with actor", System.nanoTime() - start, counter, nThreads));

        executorService.shutdown();
    }
}
