package com.wixpress.tutorial.concurrency;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/4/12
 */
public class ThreadTest {

    private int count = 0;

    @Test
    public void runSingleThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                count += 1;
            }
        };

        Thread thread = new Thread(runnable);

        thread.start();
        thread.join();
        assertThat(count, is(1));
    }
}
