package com.coy.l2cache.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadFactory that creates daemon threads for the async loggers.
 */
public class DaemonThreadFactory implements ThreadFactory {

    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String threadNamePrefix;

    public DaemonThreadFactory(final String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
        final SecurityManager securityManager = System.getSecurityManager();
        group = (securityManager != null) ? securityManager.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(group, runnable, threadNamePrefix
                + threadNumber.getAndIncrement(), 0);
        if (!thread.isDaemon()) {
            thread.setDaemon(true);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }

}