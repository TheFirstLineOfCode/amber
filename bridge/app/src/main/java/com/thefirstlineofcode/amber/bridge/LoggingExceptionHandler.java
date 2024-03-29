package com.thefirstlineofcode.amber.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingExceptionHandler.class);
    private final Thread.UncaughtExceptionHandler mDelegate;

    public LoggingExceptionHandler(Thread.UncaughtExceptionHandler delegate) {
        mDelegate = delegate;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        LOG.error("Uncaught exception: " + ex.getMessage(), ex);
        // flush the log buffers and stop logging
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();

        if (mDelegate != null) {
            mDelegate.uncaughtException(thread, ex);
        } else {
            System.exit(1);
        }
    }
}
