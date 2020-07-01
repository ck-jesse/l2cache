package com.coy.l2cache.cache.builder;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CacheExpiredListener;

/**
 * 用于构建cache对象
 *
 * @author chenck
 * @date 2020/7/1 20:43
 */
public interface CacheBuilder {

    /**
     * 构建cache对象
     */
    Cache buildCache(String name, CacheExpiredListener listener);
}
