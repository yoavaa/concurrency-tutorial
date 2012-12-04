package com.wixpress.tutorial.concurrency;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Iterator;

/**
 * @author yoav
 * @since 12/4/12
 */
public class AsyncMatchers {

    public static <T> Matcher<Iterable<? extends T>> eventually(final Matcher<T> m) {
        return new TypeSafeMatcher<Iterable<? extends T>>() {

            public boolean matchesSafely(Iterable<? extends T> iterable) {
                Iterator<? extends T> iterator = iterable.iterator();
                while (iterator.hasNext()) {
                    if (m.matches(iterator.next()))
                        return true;
                }
                return false;
            }

            public void describeTo(Description description) {
                description.appendText("retrying( ")
                        .appendDescriptionOf(m)
                        .appendText(" )");
            }
        };
    }


    public static <T> Iterable<T> overTime(final Sampler<T> sampler, final long samplingInterval, final long timeout) {
        return new Iterable<T>() {

            long start = System.currentTimeMillis();
            private T lastSample = null;

            public String toString() {
                return String.format("sampling every %,dmSec with timeout %,dmSec, last sampled value <%s>", samplingInterval, timeout, lastSample);
            }

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    boolean firstSample = true;

                    public T next() {
                        if (firstSample)
                            firstSample = false;
                        try {
                            Thread.sleep(samplingInterval);
                        }
                        catch (InterruptedException e){
                            //
                        }
                        lastSample = sampler.Sample();
                        return lastSample;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    public boolean hasNext() {
                        return firstSample || System.currentTimeMillis() - start < timeout;
                    }
                };
            }
        };

    }

    public interface Sampler<T> {
        public T Sample();
    }
}
