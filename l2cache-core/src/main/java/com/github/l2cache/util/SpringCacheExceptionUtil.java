package com.github.l2cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

/**
 * spring cache 异常包装类
 *
 * @author chenck
 * @date 2020/9/18 9:47
 */
public class SpringCacheExceptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(SpringCacheExceptionUtil.class);

    /**
     * 异常包装
     * <p>
     * 与spring cache集成时，需要包装为spring自定义的异常，否则会报类型转换异常
     * 先在CacheAspectSupport.execute()捕获Cache.ValueRetrievalException，再强转为CacheOperationInvoker.ThrowableWrapper，
     * 最后在CacheInterceptor.invoke()方法中 throw CacheOperationInvoker.ThrowableWrapper.getOriginal() 原异常。
     */
    public static RuntimeException warpper(Object key, Callable valueLoader, Exception e) {
        if (e.getClass().getName().equals("org.springframework.cache.Cache$ValueRetrievalException")) {
            return (RuntimeException) e;
        }
        RuntimeException exception;
        try {
            Class<?> throwableWrapper = Class.forName("org.springframework.cache.interceptor.CacheOperationInvoker$ThrowableWrapper");
            Constructor<?> wrapperConstructor = throwableWrapper.getConstructor(Throwable.class);
            RuntimeException wrapperException = (RuntimeException) wrapperConstructor.newInstance(e);

            Class<?> valueRetrievalException = Class.forName("org.springframework.cache.Cache$ValueRetrievalException");
            Constructor<?> valueRetrievalConstructor = valueRetrievalException.getConstructor(Object.class, Callable.class, Throwable.class);
            exception = (RuntimeException) valueRetrievalConstructor.newInstance(key, valueLoader, wrapperException);
        } catch (Exception ex) {
            logger.error("exception wrapper error, key=" + key, ex);
            return new IllegalStateException(ex);
        }
        return exception;
    }
}
