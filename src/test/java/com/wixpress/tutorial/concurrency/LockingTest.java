package com.wixpress.tutorial.concurrency;

import com.wixpress.tutorial.TestLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author yoav
 * @since 12/5/12
 */
public class LockingTest {
    @Rule
    public TestLogger testLogger = new TestLogger();
    @Rule
    public TestName name= new TestName();

    private Integer count1 = 0;
    private Integer count2 = 0;
    private AtomicInteger missMatches = new AtomicInteger(0);
    private List<Future<?>> futures = new ArrayList<>();
    private long start;
    private final int nCycles = 1000;
    private final int nThreads = 100;
    private final int readWriteFactor = 10;

    private final static Object lock = new Object();
    private final static ReentrantLock reentrantLock = new ReentrantLock();
    private final static ReentrantLock fairReentrantLock = new ReentrantLock(true);
    private final static ReentrantReadWriteLock fairReadWriteLock = new ReentrantReadWriteLock(true);
    private final static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ConcurrentLinkedQueue<TaskStats> taskStats = new ConcurrentLinkedQueue<>();

    private ConcurrencyReporter reporter = new ConcurrencyReporter();

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void logTime() throws FileNotFoundException {
        testLogger.log().debug(String.format("%,d mSec", System.currentTimeMillis() - start));
        reporter.generateReport(name.getMethodName(), taskStats);
    }

    @Test
    public void withoutLocking() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(new Writer(index++)));
            else
                futures.add(executorService.submit(new Reader(index++)));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withSynchronized() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingSynchronized(new Writer(index++))));
            else
                futures.add(executorService.submit(lockUsingSynchronized(new Reader(index++))));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withReentrantLocking() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingReentrantLock(false, new Writer(index++))));
            else
                futures.add(executorService.submit(lockUsingReentrantLock(false, new Reader(index++))));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withFairReentrantLocking() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(lockUsingReentrantLock(true, new Writer(index++))));
            else
                futures.add(executorService.submit(lockUsingReentrantLock(true, new Reader(index++))));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withReadWriteLocking() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(readWriteLock(false, false, new Writer(index++))));
            else
                futures.add(executorService.submit(readWriteLock(false, true, new Reader(index++))));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }

    @Test
    public void withFairReadWriteLocking() {
        int index = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i < nCycles; i++) {
            if (i % readWriteFactor == 0)
                futures.add(executorService.submit(readWriteLock(true, false, new Writer(index++))));
            else
                futures.add(executorService.submit(readWriteLock(true, true, new Reader(index++))));
        }

        waitForAllTasksToComplete();

        assertThat(missMatches.get(), is(0));
        assertThat(count1, is(nCycles / readWriteFactor));
    }


    private void waitForAllTasksToComplete() {
        for (Future<?> future: futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                testLogger.log().debug("interrupt exception");
            } catch (ExecutionException e) {
                testLogger.log().debug("execution exception caused by {}", e.getCause());
            }
        }
    }


    private Runnable lockUsingSynchronized(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                long startTask = System.nanoTime();
                long afterLock;
                synchronized (lock) {
                    afterLock = System.nanoTime();
                    runnable.run();
                }
                taskStats.add(new TaskStats(startTask, afterLock, (Task)runnable));
            }
        };
    }

    private Runnable lockUsingReentrantLock(final boolean fair, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                long startTask = System.nanoTime();
                long afterLock;
                getLock().lock();
                try {
                    afterLock = System.nanoTime();
                    runnable.run();
                }
                finally {
                    getLock().unlock();
                }
                taskStats.add(new TaskStats(startTask, afterLock, (Task)runnable));
            }

            private ReentrantLock getLock() {
                if (fair)
                    return fairReentrantLock;
                else
                    return reentrantLock;
            }
        };
    }

    private Runnable readWriteLock(final boolean fair, final boolean readLock, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                long startTask = System.nanoTime();
                long afterLock;
                if (readLock)
                    getLock().readLock().lock();
                else
                    getLock().writeLock().lock();
                try {
                    afterLock = System.nanoTime();
                    runnable.run();
                }
                finally {
                    if (readLock)
                        getLock().readLock().unlock();
                    else
                        getLock().writeLock().unlock();
                }
                taskStats.add(new TaskStats(startTask, afterLock, (Task)runnable));
            }

            private ReentrantReadWriteLock getLock() {
                if (fair)
                    return fairReadWriteLock;
                else
                    return readWriteLock;
            }
        };
    }

    private interface Task {
        public int getIndex();
        public TaskType getType();
    }

    private class Writer implements Runnable, Task {

        private final int index;

        private Writer(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            count1 += 1;
            randomSleep();
            count2 += 1;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public TaskType getType() {
            return TaskType.write;
        }
    }

    private class Reader implements Runnable, Task {

        private final int index;

        private Reader(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            randomSleep();
            if (!count1.equals(count2))
                missMatches.incrementAndGet();
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public TaskType getType() {
            return TaskType.read;
        }
    }

    private void randomSleep() {
        try {
            long sleepTime = ThreadLocalRandom.current().nextLong(1000, 1000000);
            Thread.sleep(sleepTime/1000000, (int)sleepTime % 1000000);
        } catch (InterruptedException e) {
            testLogger.log().debug("interrupted");
        }
    }

    private class TaskStats {
        int index;
        long startTask;
        long afterLock;
        long taskCompleted;
        TaskType type;

        public TaskStats(long startTask, long afterLock, Task runnable) {
            this.index = runnable.getIndex();
            this.startTask = startTask;
            this.afterLock = afterLock;
            this.taskCompleted = System.nanoTime();
            this.type = runnable.getType();
        }

        public String toJson() {
            return String.format("{type:\"%s\", startTask: %d, afterLock: %d, taskCompleted: %d}", type, startTask, afterLock, taskCompleted);
        }
    }

    private enum TaskType {read, write}

    public class ConcurrencyReporter {

        public void generateReport(String name, Collection<TaskStats> statsInput) throws FileNotFoundException {
            List<TaskStats> stats = new ArrayList<>(statsInput);
            Collections.sort(stats, new Comparator<TaskStats>() {
                @Override
                public int compare(TaskStats o1, TaskStats o2) {
                    return o1.index - o2.index;
                }
            });

            long minTime = Long.MAX_VALUE;
            long maxTime = Long.MIN_VALUE;

            for (TaskStats stat: stats) {
                minTime = Math.min(minTime, stat.startTask);
                maxTime = Math.max(maxTime, stat.taskCompleted);
            }

            PrintWriter pw = new PrintWriter("target/" + name + ".html");
            pw.println("<html><head>");
            pw.println("<h1>" + name + "</h1>");
            pw.println("<script language=\"javascript\">");
            pw.println("  minTime = " + minTime + ";");
            pw.println("  maxTime = " + maxTime + ";");
            pw.println("  stats = [");
            for (int i=0; i < stats.size(); i++) {
                pw.println(stats.get(i).toJson() + ((i != stats.size()-1)?", ":""));
            }
            pw.println("  ];");
            pw.println("  window.onload = function() {\n" +
                    "      var target = document.getElementById(\"target\");\n" +
                    "      target.style.height = stats.length;\n" +
                    "\n" +
                    "      var graphHtml = \"\";\n" +
                    "      for (i=0; i < stats.length; i++) {\n" +
                    "          var totalTime = maxTime - minTime;\n" +
                    "          var startTaskPx = (stats[i].startTask - minTime) / totalTime * 1500;\n" +
                    "          var lockWidth = (stats[i].afterLock - stats[i].startTask) / totalTime * 1500;\n" +
                    "          var workWidth = (stats[i].taskCompleted - stats[i].afterLock) / totalTime * 1500;\n" +
                    "          graphHtml += \"<div style=\\\"position:absolute; background: #eaa; height: 1px; top:\"+i+\"px; left:\"+startTaskPx+\"px; width:\"+lockWidth+\"px\\\"></div>\"\n" +
                    "          if (stats[i].type == \"read\")\n" +
                    "            graphHtml += \"<div style=\\\"position:absolute; background: #009920; height: 1px; top:\"+i+\"px; left:\"+(startTaskPx+lockWidth)+\"px; width:\"+workWidth+\"px\\\"></div>\"\n" +
                    "          else\n" +
                    "            graphHtml += \"<div style=\\\"position:absolute; background: #ff00ff; height: 1px; top:\"+i+\"px; left:\"+(startTaskPx+lockWidth)+\"px; width:\"+workWidth+\"px\\\"></div>\"\n" +
                    "      }\n" +
                    "      target.innerHTML = graphHtml;\n" +
                    "\n" +
                    "  };");
            pw.println("</script>");
            pw.println("</head><body>");
            pw.println("<div id=\"target\" style=\"border: #000 1px solid;width:1500px;position:relative;\"></div>");
            pw.println("</body></html>");
            pw.flush();
            pw.close();
        }
    }

}
