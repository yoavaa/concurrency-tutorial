package com.wixpress.tutorial.concurrency;

/**
 * @author Yoav
 * @since 1/15/13
 */
public class BlockingQueueTests {




    class Producer implements Runnable {
        private final BlockingQueueTests queue;
        Producer(BlockingQueueTests q) { queue = q; }
        public void run() {
            long s = System.nanoTime();
            try {
                { queue.put(produce());
                }
            } catch (InterruptedException ex) { ... handle ...}
        }
        Object produce() { ... }
    }

    class Consumer implements Runnable {
        private final BlockingQueueTests queue;
        Consumer(BlockingQueueTests q) { queue = q; }
        public void run() {
            try {
                while (true) { consume(queue.take()); }
            } catch (InterruptedException ex) { ... handle ...}
        }
        void consume(Object x) { ... }
    }

}
