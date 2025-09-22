package com.github.jesse.l2cache.cache;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.load.CacheLoader;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 可作为一级缓存和二级缓存
 *
 * @author chenck
 * @date 2020/6/29 16:57
 */
public class NoneCache implements Level1Cache, Level2Cache {

    private final String cacheName;
    private final L2CacheConfig.CacheConfig cacheConfig;

    public NoneCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
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
    public CacheLoader getCacheLoader() {
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
    public void put(Object key, Object value, boolean publishMessage) {

    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public Set<Object> keys() {
        return null;
    }

    @Override
    public Collection<Object> values() {
        return null;
    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isExists(Object key) {
        return false;
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
        return cacheConfig.isAllowNullValues();
    }

    @Override
    public long getNullValueExpireTimeSeconds() {
        return 0;
    }

    @Override
    public String getInstanceId() {
        return L2CacheConfig.INSTANCE_ID;
    }

    @Override
    public String getCacheType() {
        return CacheType.NONE.name().toLowerCase();
    }

    @Override
    public long getExpireTime() {
        return -1;
    }

}
