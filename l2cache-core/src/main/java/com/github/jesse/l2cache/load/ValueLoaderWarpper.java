package com.github.jesse.l2cache.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对 valueLoader 进行包装，通过 waitRefreshNum 过滤并发执行同一个key的refresh操作
 *
 * @author chenck
 * @date 2020/9/23 11:14
 */
public class ValueLoaderWarpper implements Callable {

    private static final Logger logger = LoggerFactory.getLogger(ValueLoaderWarpper.class);

    private final String cacheName;
    private final Object key;
    /**
     * 等待refresh的计数器
     * 大于0：表示有等待执行或正在执行的refresh操作，不执行
     * 等于0：表示可以执行refresh操作
     */
    private AtomicInteger waitRefreshNum = new AtomicInteger();

    private Callable<?> valueLoader;

    public ValueLoaderWarpper(String cacheName, Object key, Callable<?> valueLoader) {
        this.cacheName = cacheName;
        this.key = key;
        this.valueLoader = valueLoader;
    }

    @Override
    public Object call() throws Exception {
        try {
            if (null == valueLoader) {
                logger.warn("valueLoader is null, return null, cacheName={}, key={}", cacheName, key);
                return null;
            }
            return valueLoader.call();
        } finally {
            // 用于过滤并发执行同一个key的refresh操作
            if (getWaitRefreshNum() > 0) {
                int beforeWaitRefreshNum = this.clearWaitRefreshNum();
                if (logger.isDebugEnabled()) {
                    logger.debug("clear waitRefreshNum, cacheName={}, key={}, beforeWaitRefreshNum={}", cacheName, key, beforeWaitRefreshNum);
                }
            }
        }
    }

    /**
     * 递增
     * 注：过滤并发执行同一个key的refresh操作，保证同一个key只有一个refresh操作在执行
     */
    public int getAndIncrement() {
        return this.waitRefreshNum.getAndIncrement();
    }

    /**
     * 清理掉等待refresh的计数器
     */
    public int clearWaitRefreshNum() {
        return this.waitRefreshNum.getAndSet(0);
    }

    public int getWaitRefreshNum() {
        return this.waitRefreshNum.get();
    }

    public Callable<?> getValueLoader() {
        return this.valueLoader;
    }

    public void setValueLoader(Callable<?> valueLoader) {
        this.valueLoader = valueLoader;
    }

    /**
     * 创建ValueLoaderWarpper实例
     */
    public static ValueLoaderWarpper newInstance(String cacheName, Object key, Callable<?> valueLoader) {
        return new ValueLoaderWarpper(cacheName, key, valueLoader);
    }

}
