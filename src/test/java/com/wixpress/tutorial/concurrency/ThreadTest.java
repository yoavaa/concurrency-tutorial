package com.wixpress.tutorial.concurrency;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/4/12
 */
public class ThreadTest {

    Logger log = LoggerFactory.getLogger(getClass());

    private int count = 0;
    private final Object lock = new Object();
    private final Object lock2 = new Object();

    @Test
    public void runSingleThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                log.debug("thread - started");
                count += 1;
                log.debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        log.debug("starting the thread");
        thread.start();
        log.debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }

}
