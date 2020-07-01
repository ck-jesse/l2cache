package com.coy.l2cache.cache;

import com.coy.l2cache.cache.sync.CacheSyncPolicy;

import java.util.concurrent.Callable;

/**
 * 可作为一级缓存和二级缓存
 *
 * @author chenck
 * @date 2020/6/29 16:57
 */
public class NoneCache implements L1Cache, L2Cache {

    private final String name;

    public NoneCache(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getLevel() {
        return "0";
    }

    @Override
    public Object getActualCache() {
        return null;
    }

    @Override
    public CacheSyncPolicy getCacheSyncPolicy() {
        return null;
    }

    @Override
    public boolean isLoadingCache() {
        return false;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {

    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void clearLocalCache(Object key) {

    }

    @Override
    public void refresh(Object key) {

    }

    @Override
    public void refreshAll() {

    }

    @Override
    public void refreshExpireCache(Object key) {

    }

    @Override
    public void refreshAllExpireCache() {

    }

    @Override
    public boolean isAllowNullValues() {
        return false;
    }

    @Override
    public long getExpireTime() {
        return -1;
    }

    @Override
    public Object buildKey(Object key) {
        return null;
    }
}
