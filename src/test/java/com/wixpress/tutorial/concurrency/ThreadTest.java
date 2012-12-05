package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.TestLogger;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/4/12
 */
public class ThreadTest {

    @Rule
    public TestLogger testLogger = new TestLogger();

    private int count = 0;
    private final Object lock = new Object();
    private final Object lock2 = new Object();

    @Test
    public void runSingleThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                testLogger.log().debug("thread - started");
                count += 1;
                testLogger.log().debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        testLogger.log().debug("starting the thread");
        thread.start();
        testLogger.log().debug("waiting for the thread to complete");
        thread.join();
        assertThat(count, is(1));
    }

}
