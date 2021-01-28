package com.coy.l2cache.cache;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 组合缓存器
 *
 * @author chenck
 * @date 2020/6/29 17:32
 */
public class CompositeCache extends AbstractAdaptingCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(CompositeCache.class);

    private static final String SPLIT = ":";

    private final CacheConfig.Composite composite;
    /**
     * 一级缓存
     */
    private final Level1Cache level1Cache;

    /**
     * 二级缓存
     */
    private final Level2Cache level2Cache;

    /**
     * 记录是否启用过一级缓存，只要启用过，则记录为true
     * <p>
     * 以下情况可能造成本地缓存与redis缓存不一致的情况 : 开启本地缓存，更新用户数据后，关闭本地缓存,更新用户信息到redis，开启本地缓存
     * 解决方法：put、evict的情况下，判断配置中心一级缓存开关已关闭且本地一级缓存开关已开启的情况下，清除一级缓存
     */
    private AtomicBoolean openedL1Cache = new AtomicBoolean();

    public CompositeCache(String cacheName, CacheConfig cacheConfig, Level1Cache level1Cache, Level2Cache level2Cache) {
        super(cacheName, cacheConfig);
        this.composite = cacheConfig.getComposite();
        this.level1Cache = level1Cache;
        this.level2Cache = level2Cache;
        if (level1Cache.isLoadingCache()) {
            // 设置level2Cache到CustomCacheLoader中，以便CacheLoader中直接操作level2Cache
            level1Cache.getCacheLoader().setLevel2Cache(level2Cache);
        }
    }

    @Override
    public String getCacheType() {
        return CacheType.COMPOSITE.name().toLowerCase();
    }

    @Override
    public CompositeCache getActualCache() {
        return this;
    }

    @Override
    public Object get(Object key) {
        Object value = null;
        // 是否开启一级缓存
        boolean ifL1Open = ifL1Open(key);
        if (ifL1Open) {
            // L1为LoadingCache，则会在CacheLoader中对L2进行了存取操作，所以此处直接返回
            if (level1Cache.isLoadingCache()) {
                return level1Cache.get(key);
            }
            // 从L1获取缓存
            value = level1Cache.get(key);
            if (value != null) {
                logger.debug("level1Cache get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
                return value;
            }
        }
        // 从L2获取缓存
        value = level2Cache.get(key);
        if (value != null && ifL1Open) {
            logger.debug("level2Cache get cache and put in level1Cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            level1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public Object getIfPresent(Object key) {
        Object value = null;
        // 是否开启一级缓存
        boolean ifL1Open = ifL1Open(key);
        if (ifL1Open) {
            // 从L1获取缓存
            value = level1Cache.getIfPresent(key);
            if (value != null) {
                logger.debug("level1Cache get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
                return value;
            }
        }
        // 从L2获取缓存
        value = level2Cache.getIfPresent(key);
        if (value != null && ifL1Open) {
            logger.debug("level2Cache get cache and put in level1Cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            level1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 是否开启一级缓存
        if (ifL1Open(key)) {
            return level1Cache.get(key, valueLoader);
        } else {
            return level2Cache.get(key, valueLoader);
        }
    }

    @Override
    public void put(Object key, Object value) {
        level2Cache.put(key, value);
        // 是否开启一级缓存
        if (ifL1Open(key)) {
            level1Cache.put(key, value);
        }
        // 是否需要清除一级缓存
        this.ifEvictL1Cache(key);
    }

    @Override
    public void evict(Object key) {
        logger.debug("[CompositeCache] evict cache, cacheName={}, key={}", this.getCacheName(), key);
        // 先清除L2中缓存数据，然后清除L1中的缓存，避免短时间内如果先清除L1缓存后其他请求会再从L2里加载到L1中
        level2Cache.evict(key);
        // 是否开启一级缓存
        if (ifL1Open(key)) {
            level1Cache.evict(key);
        }
        // 是否需要清除一级缓存
        this.ifEvictL1Cache(key);
    }

    @Override
    public void clear() {
        logger.debug("[CompositeCache] clear all cache, cacheName={}", this.getCacheName());
        // 先清除L2中缓存数据，然后清除L1中的缓存，避免短时间内如果先清除L1缓存后其他请求会再从L2里加载到L1中
        level2Cache.clear();
        level1Cache.clear();
    }

    @Override
    public boolean isExists(Object key) {
        if (ifL1Open(key) && level1Cache.isExists(key)) {
            return true;
        }
        if (level2Cache.isExists(key)) {
            return true;
        }
        return false;
    }

    public Level1Cache getLevel1Cache() {
        return level1Cache;
    }

    public Level2Cache getLevel2Cache() {
        return level2Cache;
    }

    /**
     * 查询是否开启一级缓存
     *
     * @param key 缓存key
     * @return
     */
    private boolean ifL1Open(Object key) {
        // 检测开关与缓存名称
        if (ifL1Open()) {
            return true;
        }

        // 检测key
        return ifL1OpenByKey(key);
    }


    /**
     * 本地缓存检测，检测开关与缓存名称
     *
     * @return
     */
    private boolean ifL1Open() {
        // 判断是否开启过本地缓存
        if (composite.isL1AllOpen() || composite.isL1Manual()) {
            openedL1Cache.compareAndSet(false, true);
        }
        // 是否启用一级缓存
        if (composite.isL1AllOpen()) {
            return true;
        }
        // 是否使用手动匹配开关
        if (composite.isL1Manual()) {
            // 手动匹配缓存名字集合，针对cacheName维度
            Set<String> l1ManualCacheNameSet = composite.getL1ManualCacheNameSet();
            if (!CollectionUtils.isEmpty(l1ManualCacheNameSet) && composite.getL1ManualCacheNameSet().contains(this.getCacheName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 本地缓存检测，检测key
     *
     * @param key
     * @return
     */
    private boolean ifL1OpenByKey(Object key) {
        // 是否使用手动匹配开关
        if (composite.isL1Manual()) {
            // 手动匹配缓存key集合，针对单个key维度
            Set<String> l1ManualKeySet = composite.getL1ManualKeySet();
            if (!CollectionUtils.isEmpty(l1ManualKeySet) && l1ManualKeySet.contains(buildKeyBase(key))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建基础缓存key
     * 注：用于操作基本的缓存key
     */
    private Object buildKeyBase(Object key) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        StringBuilder sb = new StringBuilder(this.getCacheName()).append(SPLIT);
        sb.append(key.toString());
        return sb.toString();
    }

    /**
     * 是否需要清除一级缓存
     *
     * @return
     */
    private void ifEvictL1Cache(Object key) {
        // 是否关闭配置中心一级缓存开关
        boolean ifCloseLocalCache = !composite.isL1AllOpen() && !composite.isL1Manual();
        // 已关闭配置中心一级缓存开关，但曾经开启过本地一级缓存开关
        if (ifCloseLocalCache && openedL1Cache.get()) {
            logger.debug("[CompositeCache] evict l1Cache, cacheName={}, key={}", this.getCacheName(), key);
            level1Cache.evict(key);
        }
    }

    @Override
    public <T> Map<String, T> batchGet(List<String> keyList) {
        Map<String, T> resultMap = new HashMap<>();
        Set<String> keySet = new HashSet<>(keyList);
        // 查找一级缓存
        keyList.forEach(key -> {
            if (this.ifL1Open(key)) {
                Object value = level1Cache.getIfPresent(key);
                if (value != null) {
                    resultMap.put(key, (T) value);
                    keySet.remove(key);
                }
            }
        });
        // 一级缓存找到全部结果
        if (CollectionUtils.isEmpty(keySet)) {
            return resultMap;
        }
        // 查找二级缓存
        Map<String, Object> l2ResultMap = level2Cache.batchGet(new ArrayList<>(keySet));
        if (l2ResultMap != null && l2ResultMap.size() > 0) {
            l2ResultMap.forEach((key, value) -> resultMap.put(key, (T) value));
        }
        return resultMap;
    }

    @Override
    public <K, V> Map<K, V> batchGet(List<K> keyList, Function<Object, K> keyBuilder) {
        return this.batchGetOrLoad(keyList, keyBuilder, null);
    }

    @Override
    public <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<Object, K> keyBuilder, Function<List<K>, Map<K, V>> valueLoader) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO，结构 <K, cacheKey>
        Map<K, Object> keyMap = new HashMap<>();
        keyList.forEach(key -> keyMap.put(key, keyBuilder.apply(key)));

        // 获取到一级缓存中查询的key
        Map<K, Object> l1KeyMap = new HashMap<>();
        if (ifL1Open()) {
            l1KeyMap.putAll(keyMap);
        } else {
            keyMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getValue())).forEach(entry -> l1KeyMap.put(entry.getKey(), entry.getValue()));
        }

        // 缓存命中列表
        Map<K, V> hitMap = Maps.newHashMap();
        // 未命中列表
        Map<K, Object> notHitKeyMap = new HashMap<>();

        // 一级缓存批量查询
        if (!CollectionUtils.isEmpty(l1KeyMap)) {
            Map<K, V> l1HitMap = level1Cache.batchGetOrLoad(keyList, keyBuilder, valueLoader);
            hitMap.putAll(l1HitMap);
            // 获取未命中列表
            keyMap.entrySet().stream().filter(entry -> !l1HitMap.containsKey(entry.getKey())).forEach(entry -> notHitKeyMap.put(entry.getKey(), entry.getValue()));
            logger.info("[CompositeCache] batchGetOrLoad l1Cache, cacheName={}, l1KeyMap={}, l1HitMap={},  l1NotHitMap={}", this.getCacheName(), l1KeyMap, hitMap, notHitKeyMap);
        } else {
            // 获取未命中列表
            notHitKeyMap.putAll(keyMap);
            logger.info("[CompositeCache] batchGetOrLoad l1Cache not hit, cacheName={}, notHitKeyMap={}", this.getCacheName(), l1KeyMap);
        }

        // 一级缓存全部命中
        if (CollectionUtils.isEmpty(notHitKeyMap)) {
            logger.info("[CompositeCache] batchGetOrLoad l1Cache all hit, cacheName={}, KeyMap={}, l1HitMap={}", this.getCacheName(), keyMap, hitMap);
            return hitMap;
        }

        // 二级缓存批量查询
        Map<K, V> l2HitMap = level2Cache.batchGetOrLoad(new ArrayList<>(notHitKeyMap.keySet()), keyBuilder, valueLoader);
        logger.info("[CompositeCache] batchGetOrLoad l2Cache, cacheName={}, l2KeyMap={}, l2HitMap={}", this.getCacheName(), notHitKeyMap.keySet(), l2HitMap);
        hitMap.putAll(l2HitMap);
        logger.info("[CompositeCache] batchGetOrLoad, cacheName={}, KeyMap={}, keyMapSize={}, hitMap={}, hitMapSize={}", this.getCacheName(), keyMap, keyMap.size(), hitMap, hitMap.size());
        return hitMap;
    }
}
