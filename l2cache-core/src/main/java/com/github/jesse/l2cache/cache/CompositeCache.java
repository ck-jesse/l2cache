package com.github.jesse.l2cache.cache;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.hotkey.HotKeyFacade;
import com.github.jesse.l2cache.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final L2CacheConfig.Composite composite;
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

    /**
     * 热key类型
     * 注：从AbstractAdaptingCache中迁移过来，因为只有CompositeCache才需要做热key探测
     */
    protected String hotkeyType;

    public CompositeCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, Level1Cache level1Cache, Level2Cache level2Cache, String hotkeyType) {
        super(cacheName, cacheConfig);
        this.composite = cacheConfig.getComposite();
        this.level1Cache = level1Cache;
        this.level2Cache = level2Cache;
        if (level1Cache.isLoadingCache()) {
            // 设置level2Cache到CustomCacheLoader中，以便CacheLoader中直接操作level2Cache
            level1Cache.getCacheLoader().setLevel2Cache(level2Cache);
        }
        this.hotkeyType = hotkeyType;
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
                if (logger.isDebugEnabled()) {
                    logger.debug("level1Cache get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
                }
                return value;
            }
        }
        // 从L2获取缓存
        value = level2Cache.get(key);
        if (value != null && ifL1Open) {
            if (logger.isDebugEnabled()) {
                logger.debug("level2Cache get cache and put in level1Cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            }
            level1Cache.put(key, value, false);
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
            level1Cache.put(key, value, false);
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
        if (logger.isDebugEnabled()) {
            logger.debug("evict cache, cacheName={}, key={}", this.getCacheName(), key);
        }
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
        if (logger.isDebugEnabled()) {
            logger.debug("clear all cache, cacheName={}", this.getCacheName());
        }
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
    public <K, V> Map<K, V> batchGet(Map<K, Object> keyMap, boolean returnNullValueKey) {
        return this.batchGetOrLoadFromL1L2(keyMap, null, "batchGet", returnNullValueKey);
    }

    @Override
    public <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader, boolean returnNullValueKey) {
        return this.batchGetOrLoadFromL1L2(keyMap, valueLoader, "batchGetOrLoad", returnNullValueKey);
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
            if (!CollectionUtil.isEmpty(l1ManualCacheNameSet) && composite.getL1ManualCacheNameSet().contains(this.getCacheName())) {
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
            if (!CollectionUtil.isEmpty(l1ManualKeySet) && l1ManualKeySet.contains(buildKeyBase(key))) {
                return true;
            }
        }

        // 是否为热key
        if (HotKeyFacade.isHotkey(hotkeyType, this.getLevel1Cache().getCacheType(), this.getCacheName(), key.toString())) {
            return true;
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
        StringBuilder sb = new StringBuilder(this.getCacheName()).append(CacheConsts.SPLIT);
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
            if (logger.isDebugEnabled()) {
                logger.debug("evict l1Cache, cacheName={}, key={}", this.getCacheName(), key);
            }
            level1Cache.evict(key);
        }
    }

    /**
     * 从L1和L2中批量获取缓存数据
     */
    private <K, V> Map<K, V> batchGetOrLoadFromL1L2(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader, String methodName, boolean returnNullValueKey) {
        // 获取走一级缓存的key
        Map<K, Object> l1KeyMap = this.getL1KeyMap(keyMap, methodName);

        // 缓存命中列表
        Map<K, V> hitCacheMap = new HashMap<>();
        // 未命中的key列表
        Map<K, Object> l1NotHitKeyMap = new HashMap<>();

        // 一级缓存批量查询
        if (!CollectionUtil.isEmpty(l1KeyMap)) {
            Map<K, V> l1HitMap = level1Cache.batchGet(l1KeyMap, true);// 此处returnNullValueKey固定为true，不要修改防止缓存穿透
            hitCacheMap.putAll(l1HitMap);
            // 获取未命中列表（注意：此处以keyMap作为基础，过滤出来一级缓存中没有命中的key，分为两部分：一部分为不走一级缓存的key，另一部分为走一级缓存但是没有命中一级缓存的key）
            keyMap.entrySet().stream().filter(entry -> !l1HitMap.containsKey(entry.getKey())).forEach(entry -> l1NotHitKeyMap.put(entry.getKey(), entry.getValue()));
            LogUtil.log(logger, cacheConfig.getLogLevel(), "[CompositeCache] {} 部分key未命中L1, cacheName={}, l1NotHitKeySize={}", methodName, this.getCacheName(), l1NotHitKeyMap.size());
        } else {
            l1NotHitKeyMap.putAll(keyMap);
            LogUtil.log(logger, cacheConfig.getLogLevel(), "[CompositeCache] {} 全部key未命中L1, cacheName={}, keyMap={}", methodName, this.getCacheName(), l1NotHitKeyMap.values());
        }

        // 一级缓存全部命中
        if (CollectionUtil.isEmpty(l1NotHitKeyMap)) {
            LogUtil.log(logger, cacheConfig.getLogLevel(), "[CompositeCache] {} 全部key命中L1, cacheName={}, keyMapSize={}", methodName, this.getCacheName(), keyMap.size());
            return this.filterNullValue(hitCacheMap, returnNullValueKey);
        }

        // 二级缓存批量查询
        Map<K, V> l2HitMap = level2Cache.batchGet(l1NotHitKeyMap, true);// 此处returnNullValueKey固定为true，不要修改防止缓存穿透
        // logger.info("{} l2Cache batchGet, cacheName={}, l1NotHitKeyMapSize={}, l2HitMapSize={}", methodName, this.getCacheName(), l1NotHitKeyMap.size(), l2HitMap.size());

        if (!CollectionUtil.isEmpty(l2HitMap)) {
            hitCacheMap.putAll(l2HitMap);// 合并数据

            // 二级缓存同步到一级缓存
            if (!CollectionUtil.isEmpty(l1KeyMap)) {
                // 从二级缓存的缓存数据中过滤出来走一级缓存的数据，将其同步到一级缓存
                Map<Object, V> l2HitMapTemp = l2HitMap.entrySet().stream()
                        .filter(entry -> l1KeyMap.containsKey(entry.getKey()))
                        .collect(HashMap::new, (map, entry) -> map.put(l1KeyMap.get(entry.getKey()), entry.getValue()), HashMap::putAll);
                if (!CollectionUtil.isEmpty(l2HitMapTemp)) {
                    // 二级缓存同步到一级缓存，此时无需发送同步消息
                    logger.info("{} 将L2命中的缓存批量put到L1, cacheName={}, cacheMapSize={}, keyList={}", methodName, this.getCacheName(), l2HitMapTemp.size(), l2HitMapTemp.keySet());
                    level1Cache.batchPut(l2HitMapTemp, false);
                }
            }
        }

        // 一级缓存与二级缓存全部命中
        if (hitCacheMap.size() == keyMap.size()) {
            logger.info("{} 全部key命中L1和L2, cacheName={}, keyMapSize={}", methodName, this.getCacheName(), keyMap.size());
            return this.filterNullValue(hitCacheMap, returnNullValueKey);
        }

        // 获取未命中二级缓存的key列表
        Map<K, Object> l2NotHitKeyMap = l1NotHitKeyMap.entrySet().stream()
                .filter(entry -> !l2HitMap.containsKey(entry.getKey()))
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);

        if (null == valueLoader) {
            LogUtil.log(logger, cacheConfig.getLogLevel(), "[CompositeCache] {} 部分key未命中L1和L2, 且valueLoader为null，返回命中的缓存, cacheName={}, hitCacheMapSize={}, l2NotHitKeyList={}", methodName, this.getCacheName(), hitCacheMap.size(), l2NotHitKeyMap.values());
            return this.filterNullValue(hitCacheMap, returnNullValueKey);
        }

        Map<K, V> valueLoaderHitMap = this.loadAndPut(valueLoader, l2NotHitKeyMap);
        if (!CollectionUtil.isEmpty(valueLoaderHitMap)) {
            hitCacheMap.putAll(valueLoaderHitMap);// 合并数据
        }
        return this.filterNullValue(hitCacheMap, returnNullValueKey);
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
            if (logger.isDebugEnabled()) {
                logger.debug("{} 全部key先走本地缓存, cacheName={}, l1KeyMap={}", methodName, this.getCacheName(), l1KeyMap.values());
            }
        } else {
            keyMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getValue())).forEach(entry -> l1KeyMap.put(entry.getKey(), entry.getValue()));
            if (logger.isDebugEnabled()) {
                logger.debug("{} 部分key先走本地缓存, cacheName={}, l1KeyMap={}", methodName, this.getCacheName(), l1KeyMap.values());
            }
        }
        return l1KeyMap;
    }

    /**
     * 获取batchPut数据集合
     *
     * @param keyMap  key集合
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
        if (CollectionUtil.isEmpty(dataMap)) {
            return;
        }
        // 获取一级缓存数据
        Map<Object, V> l1CacheMap = new HashMap<>();
        if (ifL1Open()) {
            l1CacheMap.putAll(dataMap);
            logger.info("batchPut 全部key走本地缓存, cacheName={}, l1CacheMapSize={}", this.getCacheName(), l1CacheMap.size());
        } else {
            dataMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getKey())).forEach(entry -> l1CacheMap.put(entry.getKey(), entry.getValue()));
            logger.info("batchPut 部分key走本地缓存, cacheName={}, l1CacheMapSize={}", this.getCacheName(), l1CacheMap.size());
        }
        // 批量插入一级缓存
        if (!CollectionUtil.isEmpty(l1CacheMap)) {
            level1Cache.batchPut(l1CacheMap);
        }

        if (composite.isL2BatchPut()) {
            // 批量插入二级缓存（通过管道批量put）
            level2Cache.batchPut(dataMap);
        } else {
            // 循环put单个缓存
            // 目的：防止batchPut中通过管道批量put时连接被长时间占用，而出现无连接可用的情况出现。
            // 思考：1、是否有必要异步？2、put中是否有必要通过分布式锁来保证并发put同一个key时只有一个是成功的？
            logger.info("batchPut level2Cache start, cacheName={}, totalKeyMapSize={}", this.getCacheName(), dataMap.size());
            dataMap.entrySet().forEach(entry -> {
                level2Cache.put(entry.getKey(), entry.getValue());
            });
            logger.info("batchPut level2Cache end, cacheName={}, totalKeyMapSize={}", this.getCacheName(), dataMap.size());
        }

    }

    @Override
    public <K> void batchEvict(Map<K, Object> keyMap) {
        if (CollectionUtil.isEmpty(keyMap)) {
            return;
        }
        // 获取一级缓存key
        Map<K, Object> l1CacheMap = new HashMap<>();
        if (ifL1Open()) {
            l1CacheMap.putAll(keyMap);
            logger.info("batchEvict 全部key走本地缓存, cacheName={}, l1CacheMapSize={}", this.getCacheName(), keyMap.size());
        } else {
            keyMap.entrySet().stream().filter(entry -> ifL1OpenByKey(entry.getValue())).forEach(entry -> l1CacheMap.put(entry.getKey(), entry.getValue()));
            logger.info("batchEvict 部分key走本地缓存, cacheName={}, l1CacheMapSize={}", this.getCacheName(), l1CacheMap.size());
        }

        // 批量删除一级缓存
        if (!CollectionUtil.isEmpty(l1CacheMap)) {
            level1Cache.batchEvict(l1CacheMap);
        }

        if (composite.isL2BatchEvict()) {
            // 批量删除二级缓存（通过管道批量evict）
            level2Cache.batchEvict(keyMap);
        } else {
            // 循环evict单个缓存
            logger.info("batchEvict level2Cache start, cacheName={}, totalKeyMapSize={}", this.getCacheName(), l1CacheMap.size());
            l1CacheMap.entrySet().forEach(entry -> {
                level2Cache.evict(entry.getValue());
            });
            logger.info("batchEvict level2Cache end, cacheName={}, totalKeyMapSize={}", this.getCacheName(), l1CacheMap.size());
        }
    }

}
