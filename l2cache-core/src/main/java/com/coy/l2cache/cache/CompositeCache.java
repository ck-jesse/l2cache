package com.coy.l2cache.cache;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Override
    public <K, V> Map<K, V> batchGet(Map<K, Object> keyMap) {
        return this.batchGetOrLoadFromL1L2(keyMap, null, "batchGet");
    }

    @Override
    public <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader) {
        return this.batchGetOrLoadFromL1L2(keyMap, valueLoader, "batchGetOrLoad");
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

    /**
     * 从L1和L2中批量获取缓存数据
     */
    private <K, V> Map<K, V> batchGetOrLoadFromL1L2(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader, String methodName) {
        // 获取走一级缓存的key
        Map<K, Object> l1KeyMap = this.getL1KeyMap(keyMap, methodName);

        // 缓存命中列表
        Map<K, V> hitMap = new HashMap<>();
        // 未命中列表
        Map<K, Object> notHitKeyMap = new HashMap<>();

        // 一级缓存批量查询
        if (!CollectionUtils.isEmpty(l1KeyMap)) {
            Map<K, V> l1HitMap = level1Cache.batchGet(l1KeyMap);
            // 合并数据
            hitMap.putAll(l1HitMap);
            // 获取未命中列表
            keyMap.entrySet().stream().filter(entry -> !l1HitMap.containsKey(entry.getKey())).forEach(entry -> notHitKeyMap.put(entry.getKey(), entry.getValue()));
            logger.info("[CompositeCache] {} l1Cache, cacheName={}, l1KeyMap={}, l1HitMap={},  l1NotHitKeyMap={}", methodName, this.getCacheName(), l1KeyMap, hitMap, notHitKeyMap);
        } else {
            // 获取未命中列表
            notHitKeyMap.putAll(keyMap);
            logger.info("[CompositeCache] {} l1Cache not hit, cacheName={}, notHitKeyMap={}", methodName, this.getCacheName(), notHitKeyMap);
        }

        // 一级缓存全部命中
        if (CollectionUtils.isEmpty(notHitKeyMap)) {
            logger.info("[CompositeCache] {} l1Cache all hit, cacheName={}, KeyMap={}, hitMap={}", methodName, this.getCacheName(), keyMap, hitMap);
            return hitMap;
        }

        // 二级缓存批量查询
        Map<K, V> l2HitMap = level2Cache.batchGet(notHitKeyMap);
        logger.info("[CompositeCache] {} l2Cache, cacheName={}, l2KeyMap={}, l2HitMap={}", methodName, this.getCacheName(), notHitKeyMap.keySet(), l2HitMap);

        // 合并数据
        hitMap.putAll(l2HitMap);

        // 二级缓存同步到一级缓存
        if (!CollectionUtils.isEmpty(l1KeyMap)) {
            level1Cache.batchPut(this.getKeyMap(l1KeyMap, l2HitMap));
        }

        // 一级缓存与二级缓存全部命中
        if (hitMap.size() == keyMap.size()) {
            logger.info("[CompositeCache] {} l1Cache and l2Cache all hit, cacheName={}", methodName, this.getCacheName());
            return hitMap;
        }

        // 二级缓存未命中列表
        Map<K, Object> l2NotHitKeyMap = new HashMap<>();
        // 获取二级缓存未命中列表
        notHitKeyMap.entrySet().stream().filter(entry -> !l2HitMap.containsKey(entry.getKey())).forEach(entry -> l2NotHitKeyMap.put(entry.getKey(), entry.getValue()));

        // 数据加载命中列表
        Map<K, V> valueLoaderHitMap = valueLoader.apply(new ArrayList<>(l2NotHitKeyMap.keySet()));
        // 数据加载一个都没有命中，直接返回
        if (CollectionUtils.isEmpty(valueLoaderHitMap)) {
            // 对未命中的key缓存空值，防止缓存穿透
            l2NotHitKeyMap.forEach((k, cacheKey) -> {
                logger.info("[CompositeCache] {} notHitKey is not exist, put null, cacheName={}, cacheKey={}", methodName, this.getCacheName(), cacheKey);
                this.put(cacheKey, null);
            });
            return hitMap;
        }

        // 合并数据
        hitMap.putAll(valueLoaderHitMap);

        // 数据加载数据同步到一级缓存与二级缓存
        this.batchPut(this.getKeyMap(keyMap, valueLoaderHitMap));

        // 将未命中缓存的数据按照cacheKey的方式来组装以便put到缓存
        Map<Object, V> batchPutDataMap = new HashMap<>();
        valueLoaderHitMap.entrySet().stream().forEach(entry -> batchPutDataMap.put(keyMap.get(entry.getKey()), entry.getValue()));

        // 将未命中缓存的数据put到缓存
        this.batchPut(batchPutDataMap);
        logger.info("batchGetOrLoad batch put not hit cache data, cacheName={}, notHitKeyMap={}", this.getCacheName(), notHitKeyMap);

        // 处理没有查询到数据的key，缓存空值
        if (valueLoaderHitMap.size() != notHitKeyMap.size()) {
            notHitKeyMap.forEach((k, cacheKey) -> {
                if (!valueLoaderHitMap.containsKey(k)) {
                    logger.info("batchGetOrLoad key is not exist, put null, cacheName={}, cacheKey={}", this.getCacheName(), cacheKey);
                    this.put(cacheKey, null);
                }
            });
        }
        logger.info("[CompositeCache] {}, cacheName={}, KeyMap={}, keyMapSize={}, hitMap={}, hitMapSize={}", methodName, this.getCacheName(), keyMap, keyMap.size(), hitMap, hitMap.size());
        return hitMap;
    }

    /**
     * 获取一级缓存key
     *
     * @param keyMap     数据集合
     * @param methodName 方法名，用于日志打印
     * @return
     */
    private <K> Map<K, Object> getL1KeyMap(Map<K, Object> keyMap, String methodName) {
        Map<K, Object> l1KeyMap = new HashMap<>();
        if (ifL1Open()) {
            l1KeyMap.putAll(keyMap);
            logger.info("[CompositeCache] {} 全部key先走本地缓存, cacheName={}, l1KeyMap={}", methodName, this.getCacheName(), l1KeyMap);
        } else {
            keyMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getValue())).forEach(entry -> l1KeyMap.put(entry.getKey(), entry.getValue()));
            logger.info("[CompositeCache] {} 部分key先走本地缓存, cacheName={}, l1KeyMap={}", methodName, this.getCacheName(), l1KeyMap);
        }
        return l1KeyMap;
    }

    /**
     * 获取batchPut数据集合
     * @param keyMap key集合
     * @param dataMap 数据集合
     * @return
     */
    private <K, V> Map<Object, V> getKeyMap(Map<K, Object> keyMap, Map<K, V> dataMap) {
        Map<Object, V> resultMap = new HashMap<>();
        dataMap.entrySet().stream().filter(entry -> keyMap.containsKey(entry.getKey())).forEach(entry -> resultMap.put(keyMap.get(entry.getKey()), entry.getValue()));
        return resultMap;
    }


    @Override
    public <V> void batchPut(Map<Object, V> dataMap) {
        if (CollectionUtils.isEmpty(dataMap)) {
            return;
        }
        // 获取一级缓存数据
        Map<Object, V> l1CacheMap = new HashMap<>();
        if (ifL1Open()) {
            l1CacheMap.putAll(dataMap);
            logger.info("[CompositeCache] batchPut 全部key先走本地缓存, cacheName={}, l1KeyMap={}", this.getCacheName(), l1CacheMap);
        } else {
            dataMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getKey())).forEach(entry -> l1CacheMap.put(entry.getKey(), entry.getValue()));
            logger.info("[CompositeCache] batchPut 部分key先走本地缓存, cacheName={}, l1KeyMap={}", this.getCacheName(), l1CacheMap);
        }
        // 批量插入一级缓存
        level1Cache.batchPut(l1CacheMap);

        // 批量插入二级缓存
        level2Cache.batchPut(dataMap);
    }
}
