package com.coy.l2cache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认缓存过期监听器
 *
 * @author chenck
 * @date 2020/7/2 20:18
 */
public class DefaultCacheExpiredListener implements CacheExpiredListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheExpiredListener.class);

    @Override
    public void onExpired(Object key, Object value) {
        logger.debug("[ExpiredListener] cache is expired, key={}, value={}", key, value);
    }
}
