package com.wixpress.tutorial;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoav
 * @since 12/5/12
 */
public class TestLogger implements TestRule {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                log = LoggerFactory.getLogger(description.getClassName() + "#" + description.getMethodName());
                try {
                    base.evaluate();
                }
                finally {
                    log = LoggerFactory.getLogger(getClass());
                }
            }
        };
    }

    public Logger log() {
        return log;
    }
}
