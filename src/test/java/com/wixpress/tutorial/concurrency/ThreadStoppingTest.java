package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.DoesNotWork;
import com.wixpress.tutorial.TestLogger;
import org.junit.Rule;
import org.junit.Test;
import sun.awt.windows.ThemeReader;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/6/12
 */
public class ThreadStoppingTest {

    @Rule
    public TestLogger testLogger = new TestLogger();

    private int count = 0;

    @DoesNotWork
    @Test
    public void terminateCPUThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                testLogger.log().debug("thread - started");
                for (long i=0; i < 10000000000l; i++)
                    ;
                count += 1;
                testLogger.log().debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        testLogger.log().debug("starting the thread");
        thread.start();
        Thread.sleep(10);
        testLogger.log().debug("interrupt");
        thread.interrupt();
        testLogger.log().debug("waiting for the thread to complete");
        thread.join();
        testLogger.log().debug("thread completed and joined");
        assertThat(count, is(0));

    }

    @Test
    public void terminateIOThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                testLogger.log().debug("thread - started");
                for (long i=0; i < 100l; i++)
                    Thread.sleep(1);
                count += 1;
                testLogger.log().debug("thread - completed");
                } catch (InterruptedException e) {
                    testLogger.log().debug("thread - interrupted");
                }
            }
        };

        Thread thread = new Thread(runnable);

        testLogger.log().debug("starting the thread");
        thread.start();
        Thread.sleep(10);
        testLogger.log().debug("interrupt");
        thread.interrupt();
        testLogger.log().debug("waiting for the thread to complete");
        thread.join();
        testLogger.log().debug("thread completed and joined");
        assertThat(count, is(0));

    }

    @DoesNotWork
    @Test
    public void stopCPUThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                testLogger.log().debug("thread - started");
                for (long i=0; i < 10000000000l; i++)
                    ;
                count += 1;
                testLogger.log().debug("thread - completed");
            }
        };

        Thread thread = new Thread(runnable);

        testLogger.log().debug("starting the thread");
        thread.start();
        Thread.sleep(10);
        testLogger.log().debug("interrupt");
        thread.stop();
        testLogger.log().debug("waiting for the thread to complete");
        thread.join();
        testLogger.log().debug("thread completed and joined");
        assertThat(count, is(0));

    }

    @Test
    public void stopIOThread() throws InterruptedException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                testLogger.log().debug("thread - started");
                for (long i=0; i < 100l; i++)
                    Thread.sleep(1);
                count += 1;
                testLogger.log().debug("thread - completed");
                } catch (InterruptedException e) {
                    testLogger.log().debug("thread - interrupted");
                }
            }
        };

        Thread thread = new Thread(runnable);

        testLogger.log().debug("starting the thread");
        thread.start();
        Thread.sleep(10);
        testLogger.log().debug("interrupt");
        thread.stop();
        testLogger.log().debug("waiting for the thread to complete");
        thread.join();
        testLogger.log().debug("thread completed and joined");
        assertThat(count, is(0));

    }

}
