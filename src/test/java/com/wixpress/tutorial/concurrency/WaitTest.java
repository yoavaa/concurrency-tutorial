package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.HasDeadLock;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/4/12
 */
public class WaitTest {

    Logger log = LoggerFactory.getLogger(getClass());

    private int count = 0;
    private final Object lock = new Object();
    private final Object lock2 = new Object();

    @DoesNotWork
    @Test
    public void runAndWaitTheWrongWay() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                log.debug("thread - started");
                try {
                    log.debug("thread - waiting");
                    lock.wait();
                } catch (InterruptedException e) {
                    log.debug("thread - interrupted");
                }
                count += 1;
                log.debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        log.debug("starting the thread");
        thread.start();
        assertThat(count, is(0));
        log.debug("notifying the thread to resume");
        lock.notify();
        log.debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }

    @DoesNotWork
    @Test
    public void runAndWaitTheWrongWay2() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                log.debug("thread - started");
                try {
                    synchronized (lock2) {
                        log.debug("thread - waiting");
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    log.debug("thread - interrupted");
                }
                count += 1;
                log.debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        log.debug("starting the thread");
        thread.start();
        assertThat(count, is(0));
        synchronized (lock2) {
            log.debug("notifying the thread to resume");
            lock.notify();
        }
        log.debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }

    @Ignore
    @HasDeadLock
    @Test
    public void runAndWaitWithRandomDeadlock() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                log.debug("thread - started");
                try {
                    Thread.sleep(10);
                    synchronized (lock) {
                        log.debug("thread - waiting");
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    log.debug("thread - interrupted");
                }
                count += 1;
                log.debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        log.debug("starting the thread");
        thread.start();
        assertThat(count, is(0));
        // if the call to notify is performed before the call to wait, it will not release the waiting thread
        synchronized (lock) {
            log.debug("notifying the thread to resume");
            lock.notify();
        }
        // if we called notify before wait, our thread will wait forever, and the join command will wait also forever
        log.debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }

    @Test
    public void runAndWaitUsingCountdownLatch() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                log.debug("thread - started");
                try {
                    log.debug("thread - waiting");
                    latch.await();
                } catch (InterruptedException e) {
                    log.debug("thread - interrupted");
                }
                count += 1;
                log.debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        log.debug("starting the thread");
        thread.start();
        assertThat(count, is(0));
        latch.countDown();
        log.debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }
}
