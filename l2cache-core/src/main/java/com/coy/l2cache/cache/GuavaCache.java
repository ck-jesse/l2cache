package com.coy.l2cache.cache;

import com.coy.l2cache.cache.sync.CacheSyncPolicy;

import java.util.concurrent.Callable;

/**
 * @author chenck
 * @date 2020/6/29 16:55
 */
public class GuavaCache implements L1Cache {

    private final String cacheName;

    public GuavaCache(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    @Override
    public String getLevel() {
        return "1";
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
        return true;
    }

    @Override
    public Object get(Object key) {
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
}
